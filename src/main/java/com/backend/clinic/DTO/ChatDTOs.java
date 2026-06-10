package com.backend.clinic.DTO;

public class ChatDTOs {

    public static class ChatRequest {
        private String message;
        private String sessionUuid;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionUuid() { return sessionUuid; }
        public void setSessionUuid(String sessionUuid) { this.sessionUuid = sessionUuid; }
    }

    public static class ChatResponse {
        private String reply;
        private String intent;

        public ChatResponse(String reply, String intent) {
            this.reply = reply;
            this.intent = intent;
        }

        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
    }
}
