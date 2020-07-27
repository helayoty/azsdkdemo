package azsdkdemo.java.services;

import azsdkdemo.java.lib.DataAsync;
import azsdkdemo.java.lib.Image;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.URISyntaxException;

public class QueueAsynService {

    public static Dotenv dotEnv = Dotenv.configure().directory("/Users/heba-mac/git-repo/Java-SDK").load();
    private static DataAsync data = new DataAsync();


    public static void main(String[] args) throws InterruptedException {
        data.initializeClients();

        System.out.println("Receiving Messages...");
         data.queueAsyncClient
             .receiveMessages(Integer.valueOf(dotEnv.get("AZURE_STORAGE_QUEUE_MSG_COUNT")))
             .doOnNext(queueMessageItem -> System.out.println(queueMessageItem.getMessageText()))
             .doOnNext(queueMessageItem -> getTheImage(queueMessageItem.getMessageText()))
             .doOnNext(queueMessageItem -> data.queueAsyncClient.deleteMessage(queueMessageItem.getMessageId(), queueMessageItem.getPopReceipt()))
             .doOnNext(queueMessageItem -> System.out.println("Queue Message Deleted:"+ queueMessageItem.getMessageId()))
             .subscribe(throwable -> System.out.println("Got error: " + throwable.getMessageText()));
    }

    private static void getTheImage(String message) {
        Image image = null;
        try {
            image = new ObjectMapper().readValue(message, Image.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        try {
            Image finalImage = image;
            data.formRecognizerAsyncClient
                .beginRecognizeContentFromUrl((new URI(image.getBlobUri())).toString())
                .doOnNext(operationResultListAsyncPollResponse -> {
                    try {
                        System.out.println("start analyzing...");
                        operationResultListAsyncPollResponse.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    })
                .doOnNext(operationResultListAsyncPollResponse -> operationResultListAsyncPollResponse.getValue())
                .doOnNext(operationResultListAsyncPollResponse -> analyzeTextSentiment(finalImage))
                .subscribe();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void analyzeTextSentiment (Image image){
        System.out.println("Image Text: "+ image.getText());
        data.textAnalyticsAsyncClient.analyzeSentiment(image.getText())
            .doOnNext(documentSentiment -> image.setSentiment(documentSentiment.getSentiment().toString()))
            .doOnNext(documentSentiment ->  writeToCosmos(image))
            .subscribe();
    }

    private static void writeToCosmos (Image image){
        System.out.println("Write to cosmos...");
        data.cosmosAsyncContainer.upsertItem(image)
            .doOnNext(imageCosmosItemResponse -> System.out.println(imageCosmosItemResponse.getActivityId()))
            .subscribe();
    }

}