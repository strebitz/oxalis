package eu.peppol.as2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import eu.peppol.as2.inbound.As2InboundModule;
import eu.peppol.as2.outbound.As2OutboundModule;
import eu.peppol.identifier.AccessPointIdentifier;
import eu.peppol.identifier.MessageId;
import eu.peppol.lang.OxalisTransmissionException;
import eu.peppol.persistence.MessageRepository;
import eu.peppol.statistics.RawStatisticsRepository;
import no.difi.oxalis.api.outbound.MessageSender;
import no.difi.oxalis.api.outbound.TransmissionRequest;
import no.difi.oxalis.api.outbound.TransmissionResponse;
import no.difi.oxalis.commons.http.ApacheHttpModule;
import no.difi.oxalis.commons.mode.ModeModule;
import no.difi.oxalis.commons.timestamp.TimestampModule;
import no.difi.oxalis.commons.tracing.TracingModule;
import no.difi.oxalis.test.jetty.AbstractJettyServerTest;
import no.difi.oxalis.test.security.CertificateMock;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.common.model.Header;
import no.difi.vefa.peppol.common.model.TransportProfile;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class SimpleLoadTest extends AbstractJettyServerTest {

    private static Logger logger = LoggerFactory.getLogger(SimpleLoadTest.class);

    @Override
    public Injector getInjector() {
        System.setProperty("brave.reporter", "noop");

        return Guice.createInjector(new As2TestModule(), new As2InboundModule(), new TracingModule(), new ModeModule(),
                new TimestampModule(), new As2OutboundModule(), new ApacheHttpModule(), new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(X509Certificate.class).toInstance(CertificateMock.withCN("APP_TEST"));
                        bind(AccessPointIdentifier.class).toInstance(new AccessPointIdentifier("APP_TEST"));
                        bind(RawStatisticsRepository.class).toInstance(Mockito.mock(RawStatisticsRepository.class));
                        bind(MessageRepository.class).toInstance(Mockito.mock(MessageRepository.class));
                    }
                });
    }

    @AfterClass
    @Override
    public void afterClass() throws Exception {
        System.clearProperty("brave.reporter");
        super.afterClass();
    }

    @Test
    public void simple() throws Exception {
        MessageSender messageSender = injector.getInstance(Key.get(MessageSender.class, Names.named("oxalis-as2")));

        TransmissionRequest transmissionRequest = new TransmissionRequest() {
            @Override
            public MessageId getMessageId() {
                return new MessageId();
            }

            @Override
            public Endpoint getEndpoint() {
                return Endpoint.of(TransportProfile.AS2_1_0, URI.create("http://localhost:8080/as2"), null);
            }

            @Override
            public Header getHeader() {
                return Header.newInstance();
            }

            @Override
            public InputStream getPayload() {
                return getClass().getResourceAsStream("/as2-peppol-bis-invoice-sbdh.xml");
            }
        };

        long ts = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Future<TransmissionResponse>> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++)
            futures.add(executorService.submit(() -> messageSender.send(transmissionRequest)));

        for (Future<TransmissionResponse> future : futures)
            logger.info("{}", future.get().getMessageId());

        long result = System.currentTimeMillis() - ts;
        logger.info("Sent 500 messages in {} ms.", result);

        Assert.assertTrue(result < 60000, "Sending 500 messages took more than one minute.");
    }
}