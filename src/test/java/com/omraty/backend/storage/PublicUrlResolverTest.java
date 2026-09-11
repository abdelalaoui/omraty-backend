package com.omraty.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicUrlResolverTest {

    @Test
    void toPublicUrl_withLocalProvider_returnsStoredKeyUnchanged() {
        PublicUrlResolver resolver = new PublicUrlResolver("local", "bucket", "eu-west-3");

        assertThat(resolver.toPublicUrl("/uploads/banner/x.jpg"))
                .isEqualTo("/uploads/banner/x.jpg");
    }

    @Test
    void toPublicUrl_withS3Provider_buildsTheBucketUrl() {
        PublicUrlResolver resolver = new PublicUrlResolver("s3", "omraty-assets", "eu-west-3");

        assertThat(resolver.toPublicUrl("banner/x.jpg"))
                .isEqualTo("https://omraty-assets.s3.eu-west-3.amazonaws.com/banner/x.jpg");
    }

    @Test
    void toPublicUrl_withS3ProviderInAnyCase_stillBuildsTheBucketUrl() {
        PublicUrlResolver resolver = new PublicUrlResolver("S3", "omraty-assets", "eu-west-3");

        assertThat(resolver.toPublicUrl("banner/x.jpg"))
                .isEqualTo("https://omraty-assets.s3.eu-west-3.amazonaws.com/banner/x.jpg");
    }
}
