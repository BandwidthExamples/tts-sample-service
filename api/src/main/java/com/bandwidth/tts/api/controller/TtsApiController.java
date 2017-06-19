package com.bandwidth.tts.api.controller;

import java.io.File;
import java.security.Principal;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.bandwidth.tts.api.model.ErrorBody;
import com.bandwidth.tts.service.VendorService;
import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping("/v1")
@Api(value = "tts", description = "the tts API")
public class TtsApiController {
    private final VendorService vendorService;
    private static final Logger LOG = LoggerFactory.getLogger(TtsApiController.class);

    @Autowired
    public TtsApiController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @RequestMapping(value = "/tts", method = RequestMethod.GET)
    @ApiOperation(value = "Get audio stream for requested text",
        notes = "The text to speech service takes any text and converts that text into an audio byte stream according to the chosen audio format and voice.",
        response = File.class, tags = {"TTS"})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Byte stream of audio requested"),
        @ApiResponse(code = 400, message = "Invalid request caused by bad parameters", response = ErrorBody.class),
        @ApiResponse(code = 401, message = "Unauthorized", response = ErrorBody.class),
        @ApiResponse(code = 200, message = "Unexpected error", response = ErrorBody.class)})
    public ResponseEntity<?> ttsGet(@ApiParam(value = "Text to convert to an utterance.  This text is limited to 600 characters.", required = true) @RequestParam(value = "text") final String text,
                                    @ApiParam(value = "Voice for utterance", required = true) @RequestParam(value = "voice") final String voice,
                                    @ApiParam(value = "Audio file format for utterance.  Supported formats are PCM, MP3 and Ogg Vorbis", required = true, allowableValues = "PCM, MP3, OGG") @RequestParam(value = "format") final String format,
                                    final HttpServletResponse httpServletResponse, @ApiIgnore final Principal principal) {
        long startTime = System.currentTimeMillis();

        // Intentionally excluding validation to help test Hystrix capabilities with bad data
        LOG.info("text = {}, voice = {}, format = {}", StringUtils.truncate(text, 100), voice, format);

        final VoiceNames voiceName = VoiceNames.valueOf(voice);
        final AudioFormats audioFormat = AudioFormats.valueOf(format);
        httpServletResponse.setContentType(audioFormat.getMimeType());

        final VendorResponse vendorResponse;
        try {
            vendorResponse = vendorService.textToSpeech(text, voiceName, audioFormat, httpServletResponse.getOutputStream());
        } catch (final Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (vendorResponse == null) {
            return createErrorResponseEntity(HttpStatus.SERVICE_UNAVAILABLE, "Could not reach any of the configured text to speech vendors");
        }

        if (vendorResponse.getOutputStream().toByteArray().length == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        LOG.info("Request text = {}, voice = {}, format = {}, completed in {}ms", StringUtils.truncate(text, 100), voice, format, (System.currentTimeMillis() - startTime));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private ResponseEntity<ErrorBody> createErrorResponseEntity(final HttpStatus httpStatus, final String message) {
        LOG.info(message);
        return ResponseEntity.status(httpStatus)
                             .body(new ErrorBody().message(message));
    }
}
