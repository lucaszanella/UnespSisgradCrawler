package com.lucaszanella;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Created by lucaszanella on 8/16/16.
 * Implemented as seen here: http://stackoverflow.com/questions/26509107/how-to-specify-a-default-user-agent-for-okhttp-2-x-requests
 */
public class UserAgentInterceptor implements Interceptor {
    private final String userAgent;

    public UserAgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}
