package com.prueba.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPublishEventWhenWebhookReceivesPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "PDF content bytes".getBytes()
        );
        MockMultipartFile eventType = new MockMultipartFile(
                "eventType",
                "eventType",
                "text/plain",
                "PDF_EVENT".getBytes()
        );
        MockMultipartFile source = new MockMultipartFile(
                "source",
                "source",
                "text/plain",
                "integration-test".getBytes()
        );

        mockMvc.perform(multipart("/webhook/test")
                        .file(file)
                        .file(eventType)
                        .file(source))
                .andExpect(status().isAccepted());
    }
}
