/**
 * WalletLah Gmail ingest automation for Google Apps Script.
 *
 * Required script properties:
 * - WALLETLAH_API_BASE_URL=https://your-railway-service.up.railway.app
 * - WALLETLAH_INGEST_TOKEN=the same value as EMAIL_INGEST_TOKEN on Railway
 * - WALLETLAH_RECIPIENT_EMAIL=the email linked in Telegram with /email
 *
 * Optional script properties:
 * - WALLETLAH_GMAIL_QUERY=custom Gmail search query
 * - WALLETLAH_MAX_THREADS=20
 * - WALLETLAH_BODY_MAX_CHARS=12000
 * - WALLETLAH_MARK_REJECTED_PROCESSED=true
 * - WALLETLAH_PROCESSED_LABEL=WalletLahProcessed
 * - WALLETLAH_REJECTED_LABEL=WalletLahRejected
 * - WALLETLAH_ERROR_LABEL=WalletLahError
 */

const DEFAULT_WALLETLAH_GMAIL_QUERY =
  "newer_than:30d {spent transaction purchase card charged debited receipt DBS OCBC UOB Trust YouTrip Revolut}";

function setupWalletLahGmailIngest() {
  const config = readWalletLahConfig();
  getOrCreateLabel(config.processedLabel);
  getOrCreateLabel(config.rejectedLabel);
  getOrCreateLabel(config.errorLabel);

  const existingTriggers = ScriptApp.getProjectTriggers()
    .filter((trigger) => trigger.getHandlerFunction() === "runWalletLahGmailIngest");

  if (existingTriggers.length === 0) {
    ScriptApp.newTrigger("runWalletLahGmailIngest")
      .timeBased()
      .everyMinutes(15)
      .create();
  }

  console.log("WalletLah Gmail ingest setup complete.");
  console.log("Endpoint: " + config.endpointUrl);
  console.log("Query: " + config.gmailQuery);
}

function runWalletLahGmailIngest() {
  const config = readWalletLahConfig();
  const labels = {
    processed: getOrCreateLabel(config.processedLabel),
    rejected: getOrCreateLabel(config.rejectedLabel),
    error: getOrCreateLabel(config.errorLabel),
  };
  const threads = GmailApp.search(config.gmailQuery, 0, config.maxThreads);
  const stats = {
    threadsScanned: threads.length,
    messagesSubmitted: 0,
    duplicates: 0,
    rejected: 0,
    failed: 0,
    skippedAlreadyHandled: 0,
    skippedBlankBody: 0,
  };

  threads.forEach((thread) => {
    thread.getMessages().forEach((message) => {
      processWalletLahMessage(message, thread, config, labels, stats);
    });
  });

  console.log("WalletLah Gmail ingest finished: " + JSON.stringify(stats));
  return stats;
}

function testWalletLahEmailIngest() {
  const config = readWalletLahConfig();
  const payload = {
    recipientEmail: config.recipientEmail,
    sender: "walletlah-test@example.com",
    subject: "WalletLah test card transaction",
    body: "You have spent SGD 4.50 at KOUFU on 18 Jun 2026.",
    messageId: "apps-script-test-" + new Date().toISOString(),
  };
  const result = postToWalletLah(config, payload);
  console.log("WalletLah test result: " + JSON.stringify(result));
  return result;
}

function processWalletLahMessage(message, thread, config, labels, stats) {
  const gmailMessageId = message.getId();
  const sourceMessageId = "gmail:" + gmailMessageId;

  if (wasWalletLahMessageHandled(sourceMessageId)) {
    stats.skippedAlreadyHandled += 1;
    return;
  }

  const body = truncateText(message.getPlainBody(), config.bodyMaxChars);
  if (!body || body.trim().length === 0) {
    stats.skippedBlankBody += 1;
    markWalletLahMessageHandled(sourceMessageId, "blank_body");
    return;
  }

  const payload = {
    recipientEmail: config.recipientEmail,
    sender: truncateText(message.getFrom(), 500),
    subject: truncateText(message.getSubject(), 1000),
    body: body,
    messageId: sourceMessageId,
  };

  try {
    const result = postToWalletLah(config, payload);
    if (result.httpStatus >= 200 && result.httpStatus < 300) {
      markWalletLahMessageHandled(sourceMessageId, result.status || "submitted");
      labels.processed.addToThread(thread);
      if (result.status === "duplicate") {
        stats.duplicates += 1;
      } else {
        stats.messagesSubmitted += 1;
      }
      return;
    }

    if (result.httpStatus === 400) {
      labels.rejected.addToThread(thread);
      stats.rejected += 1;
      if (config.markRejectedProcessed) {
        markWalletLahMessageHandled(sourceMessageId, result.status || "rejected");
      }
      console.warn("WalletLah rejected message " + sourceMessageId + ": " + result.message);
      return;
    }

    labels.error.addToThread(thread);
    stats.failed += 1;
    console.error("WalletLah ingest failed for " + sourceMessageId + ": " + JSON.stringify(result));
  } catch (error) {
    labels.error.addToThread(thread);
    stats.failed += 1;
    console.error("WalletLah ingest exception for " + sourceMessageId + ": " + error);
  }
}

function postToWalletLah(config, payload) {
  const response = UrlFetchApp.fetch(config.endpointUrl, {
    method: "post",
    contentType: "application/json",
    headers: {
      "X-WalletLah-Ingest-Token": config.ingestToken,
    },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true,
  });

  const text = response.getContentText();
  const parsed = parseJsonOrEmpty(text);
  return {
    httpStatus: response.getResponseCode(),
    status: parsed.status || null,
    message: parsed.message || text,
    rawBody: text,
  };
}

function readWalletLahConfig() {
  const props = PropertiesService.getScriptProperties();
  const apiBaseUrl = requiredScriptProperty(props, "WALLETLAH_API_BASE_URL").replace(/\/+$/, "");
  const ingestToken = requiredScriptProperty(props, "WALLETLAH_INGEST_TOKEN");
  const recipientEmail = requiredScriptProperty(props, "WALLETLAH_RECIPIENT_EMAIL");

  return {
    endpointUrl: apiBaseUrl + "/api/email-expenses",
    ingestToken: ingestToken,
    recipientEmail: recipientEmail,
    gmailQuery: props.getProperty("WALLETLAH_GMAIL_QUERY") || DEFAULT_WALLETLAH_GMAIL_QUERY,
    maxThreads: readPositiveInteger(props, "WALLETLAH_MAX_THREADS", 20),
    bodyMaxChars: readPositiveInteger(props, "WALLETLAH_BODY_MAX_CHARS", 12000),
    markRejectedProcessed: readBoolean(props, "WALLETLAH_MARK_REJECTED_PROCESSED", true),
    processedLabel: props.getProperty("WALLETLAH_PROCESSED_LABEL") || "WalletLahProcessed",
    rejectedLabel: props.getProperty("WALLETLAH_REJECTED_LABEL") || "WalletLahRejected",
    errorLabel: props.getProperty("WALLETLAH_ERROR_LABEL") || "WalletLahError",
  };
}

function requiredScriptProperty(props, key) {
  const value = props.getProperty(key);
  if (!value || value.trim().length === 0) {
    throw new Error("Missing required script property: " + key);
  }
  return value.trim();
}

function readPositiveInteger(props, key, defaultValue) {
  const raw = props.getProperty(key);
  if (!raw) {
    return defaultValue;
  }
  const parsed = Number(raw);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(key + " must be a positive integer.");
  }
  return parsed;
}

function readBoolean(props, key, defaultValue) {
  const raw = props.getProperty(key);
  if (!raw) {
    return defaultValue;
  }
  return raw.trim().toLowerCase() === "true";
}

function getOrCreateLabel(name) {
  return GmailApp.getUserLabelByName(name) || GmailApp.createLabel(name);
}

function wasWalletLahMessageHandled(sourceMessageId) {
  return PropertiesService.getScriptProperties().getProperty(messageStateKey(sourceMessageId)) !== null;
}

function markWalletLahMessageHandled(sourceMessageId, status) {
  const state = {
    status: status,
    handledAt: new Date().toISOString(),
  };
  PropertiesService.getScriptProperties().setProperty(
    messageStateKey(sourceMessageId),
    JSON.stringify(state)
  );
}

function messageStateKey(sourceMessageId) {
  return "WL_MSG_" + sourceMessageId.replace(/[^A-Za-z0-9_]/g, "_");
}

function truncateText(value, maxLength) {
  if (!value) {
    return "";
  }
  const text = String(value);
  if (text.length <= maxLength) {
    return text;
  }
  return text.substring(0, maxLength);
}

function parseJsonOrEmpty(text) {
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return {};
  }
}
