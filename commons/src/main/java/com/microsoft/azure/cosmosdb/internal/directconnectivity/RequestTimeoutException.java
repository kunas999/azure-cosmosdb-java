/*
 * The MIT License (MIT)
 * Copyright (c) 2018 Microsoft Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.cosmosdb.internal.directconnectivity;

import com.microsoft.azure.cosmosdb.BridgeInternal;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.Error;
import com.microsoft.azure.cosmosdb.internal.HttpConstants;
import com.microsoft.azure.cosmosdb.rx.internal.RMResources;
import com.microsoft.azure.cosmosdb.rx.internal.Strings;
import io.reactivex.netty.protocol.http.client.HttpResponseHeaders;

import java.net.URI;
import java.util.Map;

public class RequestTimeoutException extends DocumentClientException {

    public RequestTimeoutException() {
        this(RMResources.RequestTimeout, null);
    }

    public RequestTimeoutException(Error error, long lsn, String partitionKeyRangeId, Map<String, String> responseHeaders) {
        super(HttpConstants.StatusCodes.REQUEST_TIMEOUT, error, responseHeaders);
        BridgeInternal.setLSN(this, lsn);
        BridgeInternal.setPartitionKeyRangeId(this, partitionKeyRangeId);
    }

    public RequestTimeoutException(String message, URI requestUri) {
        this(message, (Exception) null, (HttpResponseHeaders) null, requestUri);
    }

    public RequestTimeoutException(String message,
                                   Exception innerException,
                                   URI requestUri,
                                   String localIpAddress) {
        this(message(localIpAddress, message), innerException, (HttpResponseHeaders) null, requestUri);
    }

    public RequestTimeoutException(Exception innerException) {
        this(RMResources.Gone, innerException, (HttpResponseHeaders) null, null);
    }

    public RequestTimeoutException(String message, HttpResponseHeaders headers, URI requestUri) {
        super(message, null, HttpUtils.asMap(headers), HttpConstants.StatusCodes.REQUEST_TIMEOUT, requestUri != null ? requestUri.toString() : null);
    }

    public RequestTimeoutException(String message, HttpResponseHeaders headers, String requestUri) {
        super(message, null, HttpUtils.asMap(headers), HttpConstants.StatusCodes.REQUEST_TIMEOUT, requestUri);
    }

    public RequestTimeoutException(String message,
                                   Exception innerException,
                                   HttpResponseHeaders headers,
                                   URI requestUri) {
        super(message, innerException, HttpUtils.asMap(headers), HttpConstants.StatusCodes.REQUEST_TIMEOUT, requestUri != null ? requestUri.toString() : null);
    }

    private static String message(String localIP, String baseMessage) {
        if (!Strings.isNullOrEmpty(localIP)) {
            return String.format(
                RMResources.ExceptionMessageAddIpAddress,
                baseMessage,
                localIP);
        }

        return baseMessage;
    }
}
