package com.naveenmandal.urlshortener.controller;

import com.naveenmandal.urlshortener.dto.ShortenRequest;
import com.naveenmandal.urlshortener.dto.ShortenResponse;
import com.naveenmandal.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController  // = @Controller + @ResponseBody (returns JSON, not HTML views)
public class UrlShortenerController {

    private final UrlShortenerService shortenerService;

    public UrlShortenerController(UrlShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    /**
     * POST /shorten
     * Body: { "longUrl": "https://google.com" }
     *
     * Returns 201 Created with the short URL info.
     *
     * @RequestBody → Spring reads the JSON body and maps it to ShortenRequest
     * ResponseEntity → gives you full control over HTTP status code + body
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody ShortenRequest request) {
        ShortenResponse response = shortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /{shortCode}
     * Example: GET /1 -> redirects to https://www.google.com
     *
     * Returns:
     * - 302 Found + "Location: <longUrl>" if code exists
     * - 404 Not Found if code does not exist
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Optional<String> longUrl = shortenerService.resolve(shortCode);

        // If shortCode does not exist, return 404 Not Found
        if (longUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Build HTTP 302 Redirect with Location header
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl.get()));

        return ResponseEntity.status(HttpStatus.FOUND) // HTTP 302
                .headers(headers)
                .build();
    }
}