package com.wanted.boothub.global.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 헤더에 고정으로 노출되는 외부 링크 (boothub.links.*).
 * 실제 주소는 환경변수(BOOTHUB_LINKS_SLACK 등) 또는 application-local.yaml 에서 읽는다.
 */
@ConfigurationProperties(prefix = "boothub.links")
public record ExternalLinkProperties(String slack, String github, String notion) {

    /** 헤더에 그리는 순서대로 */
    public List<ExternalLink> all() {
        return List.of(
                new ExternalLink("Slack", slack),
                new ExternalLink("GitHub", github),
                new ExternalLink("Notion", notion));
    }

    public record ExternalLink(String label, String url) {

        public boolean configured() {
            return url != null && !url.isBlank();
        }
    }
}
