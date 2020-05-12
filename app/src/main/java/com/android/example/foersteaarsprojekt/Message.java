package com.android.example.foersteaarsprojekt;

public class Message {
    private String sender;
    private String message;

    public Message() {
    }

    public Message(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }
}
