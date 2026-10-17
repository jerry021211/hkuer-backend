package com.hkuer.hkuer.service.impl.userface;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationResponse;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.hkuer.hkuer.config.OssConfig;
import com.hkuer.hkuer.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.InputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CloudServicesTest {
    @Test void moderationFailureDoesNotPassContent() throws Exception {
        Client client = mock(Client.class);
        ContentSecurityServiceImpl service = new ContentSecurityServiceImpl(client);
        when(client.textModerationWithOptions(any(), any())).thenReturn(null);
        assertThatThrownBy(() -> service.checkText("test")).isInstanceOf(BusinessException.class);
        when(client.textModerationWithOptions(any(), any())).thenReturn(
                new TextModerationResponse().setBody(new TextModerationResponseBody().setCode(500)));
        assertThatThrownBy(() -> service.checkText("test")).isInstanceOf(BusinessException.class);
        when(client.textModerationWithOptions(any(), any())).thenThrow(new RuntimeException("network unavailable"));
        assertThatThrownBy(() -> service.checkText("test")).isInstanceOf(BusinessException.class);
    }

    @Test void pdfUploadsUseOssAndRetainMimeTypeAndOwnershipRecognition() {
        OSS client = mock(OSS.class);
        OssConfig config = new OssConfig();
        config.setBucketName("test-bucket"); config.setEndpoint("https://oss-cn-shenzhen.aliyuncs.com");
        OssServiceImpl service = new OssServiceImpl();
        ReflectionTestUtils.setField(service, "ossClient", client);
        ReflectionTestUtils.setField(service, "ossConfig", config);
        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "%PDF-1.4\ntest".getBytes());
        String url = service.uploadFile(file, "course-material");
        assertThat(url).startsWith("https://test-bucket.oss-cn-shenzhen.aliyuncs.com/course-material/").endsWith(".pdf");
        verify(client).putObject(eq("test-bucket"), anyString(), any(InputStream.class), argThat((ObjectMetadata m) -> "application/pdf".equals(m.getContentType())));
        assertThat(service.isManagedUrl(url)).isTrue();
        assertThat(service.isManagedUrl("/java/media/123.pdf")).isTrue();
        assertThat(service.isManagedUrl("https://unrelated.example/notes.pdf")).isFalse();
        assertThatThrownBy(() -> service.uploadFile(new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes()), "course-material"))
                .isInstanceOf(BusinessException.class);
        verifyNoMoreInteractions(client);
    }
}
