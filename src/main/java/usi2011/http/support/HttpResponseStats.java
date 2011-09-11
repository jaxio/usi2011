package usi2011.http.support;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public final class HttpResponseStats {
    public int ok;
    public int created;
    public int badRequest;
    public int notFound;
    public int unauthorized;
    public int internalServerError;
    public int methodNotAllowed;

    public void newResponse(final HttpResponseStatus status) {
        if (status == OK) {
            ok++;
        } else if (status == CREATED) {
            created++;
        } else if (status == BAD_REQUEST) {
            badRequest++;
        } else if (status == NOT_FOUND) {
            notFound++;
        } else if (status == UNAUTHORIZED) {
            unauthorized++;
        } else if (status == INTERNAL_SERVER_ERROR) {
            internalServerError++;
        } else if (status == METHOD_NOT_ALLOWED) {
            methodNotAllowed++;
        }
    }

    public void addCounters(HttpResponseStats other) {
        ok += other.ok;
        created += other.created;
        badRequest += other.badRequest;
        notFound += other.notFound;
        unauthorized += other.unauthorized;
        internalServerError += other.internalServerError;
        methodNotAllowed += other.methodNotAllowed;
    }

    public boolean hasHigherStatThan(HttpResponseStats other) {
        return ok > other.ok //
                || created > other.created //
                || badRequest > other.badRequest //
                || notFound > other.notFound //
                || unauthorized > other.unauthorized //
                || internalServerError > other.internalServerError //
                || methodNotAllowed > other.methodNotAllowed;
    }

    public long count() {
        return ok + created + badRequest + notFound + unauthorized + internalServerError + methodNotAllowed;
    }
}
