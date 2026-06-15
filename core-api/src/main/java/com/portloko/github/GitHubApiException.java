package com.portloko.github;

public class GitHubApiException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public GitHubApiException(String message, int statusCode, String code) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int statusCode() {
        return statusCode;
    }

    public String code() {
        return code;
    }
}
