package org.lavenderx.http;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Param;
import org.asynchttpclient.Response;
import org.asynchttpclient.SslEngineFactory;
import org.asynchttpclient.netty.ssl.InsecureTrustManagerFactory;
import org.lavenderx.http.annotation.HttpOption;
import org.lavenderx.http.databind.DefaultUnmarshaller;
import org.lavenderx.http.databind.MarshalResult;
import org.lavenderx.http.databind.Marshaller;
import org.lavenderx.http.databind.StandardMarshaller;
import org.lavenderx.http.databind.Unmarshaller;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.lavenderx.http.utils.NonNullToStringStyle.NON_NULL_JSON_STYLE;

@Slf4j
public class HttpSender {

    private final int timeout;
    private final boolean useAsync;
    private final boolean printResponseBody;
    private final Map<String, String> headers;
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    private final HttpClientBuilder httpClientBuilder;
    private final DefaultAsyncHttpClientConfig.Builder asyncHttpClientConfigBuilder;

    private HttpSender(final int timeout,
                       final boolean useAsync,
                       final boolean printResponseBody,
                       final Map<String, String> headers,
                       final Marshaller marshaller,
                       final Unmarshaller unmarshaller,
                       final HttpClientBuilder httpClientBuilder,
                       final DefaultAsyncHttpClientConfig.Builder asyncHttpClientConfigBuilder) {
        this.timeout = timeout;
        this.useAsync = useAsync;
        this.printResponseBody = printResponseBody;
        this.headers = headers;
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
        this.httpClientBuilder = httpClientBuilder;
        this.asyncHttpClientConfigBuilder = asyncHttpClientConfigBuilder;
    }

    /**
     * The default HttpSender is sync mode.
     *
     * @return an instance of {@code HttpSender}
     */
    public static HttpSender createDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public final <T> T send(String rootUrl, Object req, Object resType) throws SenderException {
        try {
            MarshalResult marshalResult = marshaller.marshal(req);
            HttpOption httpOption = req.getClass().getAnnotation(HttpOption.class);
            HttpMethod method = httpOption.method();
            ContentType contentType = ContentType.create(httpOption.mimeType(), UTF_8);
            String requestUrl = rootUrl + marshalResult.url();
            String responseString;
            switch (method) {
                case GET:
                    HttpGet getRequest = new HttpGet(requestUrl);
                    getRequest.setHeader(HttpHeaders.CONTENT_TYPE, contentType.getMimeType());
                    responseString = execute(getRequest);
                    break;
                case POST:
                    HttpPost request = new HttpPost(requestUrl);
                    if (Objects.equals(contentType.getMimeType(), ContentType.APPLICATION_JSON.getMimeType())) {
                        request.setEntity(new StringEntity(marshalResult.requestBody().toString(), contentType));
                        responseString = execute(request);
                    } else if (Objects.equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), contentType.getMimeType())) {
                        List<NameValuePair> parameters = ((Map<String, String>) marshalResult.requestBody())
                                .entrySet()
                                .stream()
                                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList());

                        request.setHeader(HttpHeaders.CONTENT_TYPE, contentType.getMimeType());
                        request.setEntity(new UrlEncodedFormEntity(parameters, UTF_8));
                        responseString = execute(request);
                    } else if (Objects.equals(ContentType.MULTIPART_FORM_DATA.getMimeType(), contentType.getMimeType())) {
                        Map<String, ContentBody> parameters = (Map<String, ContentBody>) marshalResult.requestBody();
                        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
                                .setContentType(contentType)
                                .setCharset(UTF_8);
                        parameters.forEach(entityBuilder::addPart);
                        HttpEntity httpEntity = entityBuilder.build();
                        request.setEntity(httpEntity);
                        responseString = execute(request);
                    } else {
                        throw new SenderException("Unknown Content-Type in HTTP request");
                    }
                    break;
                default:
                    throw new SenderException("Unsupported HTTP method: " + method);
            }
            if (printResponseBody) {
                log.info("Sync response Body: {}", responseString);
            }

            if (resType instanceof TypeReference<?>) {
                return unmarshaller.unmarshal(responseString, (TypeReference<T>) resType);
            } else {
                return unmarshaller.unmarshal(responseString, (Class<T>) resType);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SenderException(e);
        }
    }

    public final <T> ListenableFuture<T> sendAsync(String rootUrl, Object req, Object resType)
            throws SenderException {
        if (!useAsync) {
            throw new SenderException("Async mode isn't active");
        }

        try {
            MarshalResult marshalResult = marshaller.marshal(req);
            return executeAsync(rootUrl, req.getClass().getAnnotation(HttpOption.class), marshalResult, resType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SenderException(e);
        }
    }

    public final <T, R> R sendByXml(String url, T request, Class<T> reqClass, Class<R> resClass)
            throws SenderException {
        try {
            JAXBContext context = JAXBContext.newInstance(reqClass);

            javax.xml.bind.Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, UTF_8.name());
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, false);

            StringWriter writer = new StringWriter();
            marshaller.marshal(request, writer);
            String reqXmlString = writer.toString();
            log.info("XML Request: {}", reqXmlString);

            StringEntity entity = new StringEntity(reqXmlString, UTF_8.name());
            entity.setChunked(true);

            HttpPost postRequest = new HttpPost(url);
            postRequest.setConfig(createRequestConfig());
            postRequest.setEntity(entity);
            postRequest.setHeader(CONTENT_TYPE, ContentType.TEXT_XML.getMimeType());

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(postRequest)) {
                String resXmlString = EntityUtils.toString(response.getEntity(), UTF_8);
                log.info("XML Response: {}", resXmlString);

                javax.xml.bind.Unmarshaller unmarshaller = JAXBContext.newInstance(resClass).createUnmarshaller();

                JAXBElement<R> jaxbElement = unmarshaller
                        .unmarshal(new StreamSource(new StringReader(resXmlString)), resClass);

                return jaxbElement.getValue();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SenderException(e);
        }
    }

    protected String execute(HttpRequestBase request) throws IOException {
        request.setConfig(createRequestConfig());
        if (!headers.isEmpty()) {
            headers.forEach(request::setHeader);
        }
        if (request instanceof HttpPost) {
            log.info("Sync request: {requestLine={}, requestBody={}}",
                    ToStringBuilder.reflectionToString(request.getRequestLine(), NON_NULL_JSON_STYLE),
                    EntityUtils.toString(((HttpPost) request).getEntity(), UTF_8));
        } else {
            log.info("Sync request: {}",
                    ToStringBuilder.reflectionToString(request.getRequestLine(), NON_NULL_JSON_STYLE));
        }

        try (CloseableHttpClient client = httpClientBuilder.build();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                log.error("statusCode: {}, response: {}",
                        statusCode,
                        Objects.nonNull(response.getEntity()) ? EntityUtils.toString(response.getEntity(), UTF_8) : EMPTY);
                throw new SenderException(response.getStatusLine().getReasonPhrase());
            }
            return EntityUtils.toString(response.getEntity(), UTF_8);
        } catch (IOException e) {
            throw new SenderException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <R> ListenableFuture<R> executeAsync(String rootUrl,
                                                   HttpOption httpOption,
                                                   MarshalResult marshalResult,
                                                   Object resType) {
        try {
            final AsyncHttpClient asyncHttpClient = asyncHttpClient(asyncHttpClientConfigBuilder);
            HttpMethod method = httpOption.method();
            String mimeType = httpOption.mimeType();
            String requestUrl = rootUrl + marshalResult.url();

            final BoundRequestBuilder boundRequestBuilder = new BoundRequestBuilder(asyncHttpClient, method.toString(), true);
            boundRequestBuilder.setUrl(requestUrl)
                    .setCharset(UTF_8)
                    .setHeader(CONTENT_TYPE, mimeType);
            if (HttpMethod.POST == method) {
                if (Objects.equals(ContentType.APPLICATION_JSON.getMimeType(), mimeType)) {
                    boundRequestBuilder.setBody(marshalResult.requestBody().toString());
                } else if (Objects.equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), mimeType)) {
                    List<Param> formParams = ((Map<String, String>) marshalResult.requestBody())
                            .entrySet()
                            .stream()
                            .map(entry -> new Param(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());
                    boundRequestBuilder.setFormParams(formParams);
                } else if (Objects.equals(ContentType.MULTIPART_FORM_DATA.getMimeType(), mimeType)) {
                    // TODO send `multipart/form-data` request
                }
            }
            if (!headers.isEmpty()) {
                headers.forEach(boundRequestBuilder::setHeader);
            }
            log.info("Async request: {uri={}, method={}, stringData={}}", requestUrl, method, marshalResult.requestBody());

            ListenableFuture<R> rFuture = boundRequestBuilder.execute(new AsyncCompletionHandler<R>() {

                @Override
                public R onCompleted(Response response) throws Exception {
                    int statusCode = response.getStatusCode();
                    if (HttpStatus.SC_OK != statusCode) {
                        log.error("statusCode: {}, response: {}", statusCode, response.getStatusText());
                        throw new SenderException(response.getStatusText());
                    }

                    try {
                        String responseString = response.getResponseBody(UTF_8);
                        if (printResponseBody) {
                            log.info("Async response Body: {}", responseString);
                        }

                        if (resType instanceof TypeReference<?>) {
                            return unmarshaller.unmarshal(responseString, (TypeReference<R>) resType);
                        } else {
                            return unmarshaller.unmarshal(responseString, (Class<R>) resType);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        throw new SenderException(e);
                    } finally {
                        if (!asyncHttpClient.isClosed()) {
                            try {
                                asyncHttpClient.close();
                            } catch (IOException e) {
                                log.error("Exception on closing AsyncHttpClient", e);
                            }
                        }
                    }
                }

                @Override
                public void onThrowable(Throwable t) {
                    if (!asyncHttpClient.isClosed()) {
                        try {
                            asyncHttpClient.close();
                        } catch (IOException ignored) {
                        }
                    }

                    log.error(t.getMessage(), t);
                    throw new SenderException(t);
                }
            });

            return rFuture;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SenderException(e);
        }
    }

    private RequestConfig createRequestConfig() {
        return RequestConfig
                .custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
    }

    public static class Builder {

        private int timeout;
        private int maxRetry;
        private boolean useAsync;
        private boolean ignoreSSL;
        private boolean printResponseBody;
        private Map<String, String> headers;
        private SSLContext sslContext;
        private SSLConnectionSocketFactory sslSocketFactory;
        private SslContext nettySslContext;
        private SslEngineFactory sslEngineFactory;

        private Marshaller marshaller;
        private Unmarshaller unmarshaller;

        private final HttpClientBuilder httpClientBuilder;
        private final DefaultAsyncHttpClientConfig.Builder asyncHttpClientConfigBuilder;

        public Builder() {
            this.timeout = 60_000;
            this.maxRetry = 3;
            this.headers = Collections.emptyMap();
            this.marshaller = new StandardMarshaller();
            this.unmarshaller = new DefaultUnmarshaller();
            this.httpClientBuilder = HttpClients.custom();
            this.asyncHttpClientConfigBuilder = new DefaultAsyncHttpClientConfig.Builder();
        }

        public final Builder setTimeout(final int seconds) {
            this.timeout = seconds * 1000;
            return this;
        }

        public final Builder setMaxRetry(final int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        public final Builder useAsync() {
            this.useAsync = true;
            return this;
        }

        public final Builder ignoreSSL() {
            this.ignoreSSL = true;
            return this;
        }

        public final Builder enablePrintResponseBody() {
            this.printResponseBody = true;
            return this;
        }

        public final Builder setHeaders(final Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * @param sslContext for {@code HttpClient}
         * @return
         */
        public final Builder setSslcontext(final SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * @param sslSocketFactory for {@code HttpClient}
         * @return
         */
        public final Builder setSslSocketFactory(final SSLConnectionSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        /**
         * @param nettySslContext for {@code AsyncHttpClient}
         * @return
         */
        public final Builder setNettySslContext(final SslContext nettySslContext) {
            this.nettySslContext = nettySslContext;
            return this;
        }

        /**
         * @param sslEngineFactory for {@code AsyncHttpClient}
         * @return
         */
        public final Builder setSslEngineFactory(final SslEngineFactory sslEngineFactory) {
            this.sslEngineFactory = sslEngineFactory;
            return this;
        }

        public final Builder setMarshaller(final Marshaller marshaller) {
            this.marshaller = marshaller;
            return this;
        }

        public final Builder setUnmarshaller(final Unmarshaller unmarshaller) {
            this.unmarshaller = unmarshaller;
            return this;
        }

        public final HttpSender build() {
            if (useAsync) {
                if (ignoreSSL) {
                    try {
                        SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                        this.asyncHttpClientConfigBuilder.setSslContext(sslContext);
                    } catch (SSLException e) {
                        log.error("Ignore SSL certificate failed", e);
                        throw new SenderException("Ignore SSL certificate failed", e);
                    }
                }

                this.asyncHttpClientConfigBuilder.setConnectTimeout(timeout);
                this.asyncHttpClientConfigBuilder.setRequestTimeout(timeout);
                this.asyncHttpClientConfigBuilder.setMaxRequestRetry(maxRetry);
            } else {
                if (Objects.nonNull(nettySslContext)) {
                    this.asyncHttpClientConfigBuilder.setSslContext(nettySslContext);
                }
                if (Objects.nonNull(sslEngineFactory)) {
                    this.asyncHttpClientConfigBuilder.setSslEngineFactory(sslEngineFactory);
                }
            }

            if (ignoreSSL) {
                try {
                    SSLContext sslContext = new SSLContextBuilder()
                            .loadTrustMaterial(null, (chain, authType) -> true).build();

                    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                            sslContext,
                            new String[]{"TLSv1"},
                            null,
                            NoopHostnameVerifier.INSTANCE);

                    this.httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    log.error("Ignore SSL certificate failed", e);
                    throw new SenderException("Ignore SSL certificate failed", e);
                }
            } else {
                if (Objects.nonNull(sslContext)) {
                    this.httpClientBuilder.setSslcontext(sslContext);
                }
                if (Objects.nonNull(sslSocketFactory)) {
                    this.httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
                }
            }

            // Using `StandardHttpRequestRetryHandler` replace `DefaultHttpRequestRetryHandler`
            this.httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler(maxRetry, true));

            // Add retry strategy when service unavailable
            this.httpClientBuilder.setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
                private final Set<Integer> serviceUnavailableStatusCodes = new HashSet<>(
                        Arrays.asList(new Integer[]{
                                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                HttpStatus.SC_BAD_GATEWAY,
                                HttpStatus.SC_SERVICE_UNAVAILABLE,
                                HttpStatus.SC_GATEWAY_TIMEOUT})
                );
                private static final long retryInterval = 3_000;

                @Override
                public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                    return executionCount <= maxRetry &&
                            serviceUnavailableStatusCodes.contains(response.getStatusLine().getStatusCode());
                }

                @Override
                public long getRetryInterval() {
                    return retryInterval;
                }
            });

            // Considering every request has different `Content-Type`.
            if (headers.containsKey(CONTENT_TYPE)) {
                headers.remove(CONTENT_TYPE);
            }

            return new HttpSender(timeout, useAsync, printResponseBody, headers,
                    marshaller, unmarshaller, httpClientBuilder, asyncHttpClientConfigBuilder);
        }
    }
}

