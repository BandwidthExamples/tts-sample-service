## Bandwidth Sample Text-To-Speech Service

A bare-bones functional service meant to provide a basic framework for building a Text-to-Speech (TTS) Service backed by Amazon Polly and other vendors.

## Features
Included Features: 
* Vendor fallback and skipping with Hystrix
* Caching responses with Amazon S3
* Instant streaming, no need to wait for the full response before delivering audio.

### Warning
Error handling and data validation has been excluded intentionally to be left up to users.


### Prerequisites
Demo is built with 2 assumptions.

1) Your host has AWS credentials configured so that it can generate clients without passing in credentials. For setting up on your development host, consult:
http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html   


2) The api/main/resources/application.properties S3 properties have to be set to a bucket your user can write to.

### Running the Service

Navigate to the project root.

#### Search for existing application, if doesn't exist: create one
```bash
./gradlew build
api/build/libs/text2speech-api{version-info}.jar start
```

### Calling the Service
After running the service visit: http://localhost:8090/swagger-ui.html for a UI Experience

With curl:

```bash
curl -X GET --header 'Accept: {audio_mime_type}' 'http://{host}:8090/v1/tts?text={text}&voice={voice_name}&format={audio_format}'
```

### Sample Request and Response
```bash
curl -X GET --header 'Accept: audio/ogg' 'http://localhost:8090/v1/tts?text=Simple%20Test&voice=Kimberly&format=OGG'
```

```json
{
  "pragma": "no-cache",
  "date": "Fri, 02 Jun 2017 17:25:55 GMT",
  "x-content-type-options": "nosniff",
  "x-frame-options": "DENY",
  "content-type": "audio/ogg",
  "cache-control": "no-cache, no-store, max-age=0, must-revalidate",
  "transfer-encoding": "chunked",
  "x-xss-protection": "1; mode=block",
  "x-application-context": "Text2SpeechSampleApi:8090",
  "expires": "0"
}
```