package com.portloko.shared;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps NotFoundException → 404 with the canonical error format:
 * { "error": "...", "code": "NOT_FOUND" }
 */
@Provider
public class ErrorMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody(exception.getMessage(), "NOT_FOUND"))
                .build();
    }

    public record ErrorBody(String error, String code) {}
}
