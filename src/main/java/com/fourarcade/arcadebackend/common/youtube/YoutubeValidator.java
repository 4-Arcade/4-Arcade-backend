package com.fourarcade.arcadebackend.common.youtube;

import com.fourarcade.arcadebackend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YoutubeValidator {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile(
            "^https://(?:www\\.youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11})(?:[&?].*)?$"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    public void validateEmbeddable(String youtubeUrl) {
        extractVideoId(youtubeUrl);

        URI oEmbedUri = UriComponentsBuilder
                .fromUriString("https://www.youtube.com/oembed")
                .queryParam("url", youtubeUrl)
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();

        try {
            restTemplate.getForEntity(oEmbedUri, String.class);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new BusinessException(
                    "YOUTUBE_EMBED_BLOCKED",
                    "임베드할 수 없는 YouTube 영상입니다.",
                    HttpStatus.BAD_REQUEST
            );
        } catch (HttpClientErrorException ex) {
            throw new BusinessException(
                    "YOUTUBE_EMBED_BLOCKED",
                    "YouTube 영상을 확인할 수 없습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public String extractVideoId(String youtubeUrl) {
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(youtubeUrl);

        if (!matcher.matches()) {
            throw new BusinessException("INVALID_YOUTUBE_URL", "유효한 YouTube URL 형식이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        return matcher.group(1);
    }
}