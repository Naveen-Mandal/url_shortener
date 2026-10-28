package com.naveenmandal.urlshortener.controller;

import com.naveenmandal.urlshortener.dto.ShortenRequest;
import com.naveenmandal.urlshortener.dto.ShortenResponse;
import com.naveenmandal.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}