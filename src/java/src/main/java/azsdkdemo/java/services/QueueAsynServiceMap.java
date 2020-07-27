package azsdkdemo.java.services;

import azsdkdemo.java.lib.DataAsync;
import azsdkdemo.java.lib.Image;
import com.azure.ai.formrecognizer.models.FormLine;
import com.azure.ai.formrecognizer.models.FormPage;
import com.azure.core.util.polling.AsyncPollResponse;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.storage.queue.models.QueueMessageItem;
import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueueAsynServiceMap {

    public static Dotenv dotEnv = Dotenv.configure().directory("/Users/heba-mac/git-repo/Java-SDK").load();
    private static DataAsync data = new DataAsync();


    public static void main(String[] args) throws URISyntaxException {
        data.initializeClients();

        System.out.println("Receiving Messages...");
         data.queueAsyncClient
             .receiveMessages(Integer.valueOf(dotEnv.get("AZURE_STORAGE_QUEUE_MSG_COUNT")))
             .transform(QueueAsynServiceMap::getImage)
             .doOnNext(System.out::println)
             .transform(QueueAsynServiceMap::getFormPages)
             .doOnNext(System.out::println)
             .filter(image -> StringUtils.isNotBlank(image.getText()))
             .doOnNext(System.out::println)
             .transform(QueueAsynServiceMap::analyzeTextSentiment)
             .doOnNext(System.out::println)
             .transform(QueueAsynServiceMap::writeToCosmos)
             .subscribe(null, throwable -> System.out.println("Got error: " + throwable));
          //   .doOnNext(queueMessageItem -> data.queueAsyncClient.deleteMessage(queueMessageItem.getMessageId(), queueMessageItem.getPopReceipt()))
          //   .doOnNext(queueMessageItem -> System.out.println("Queue Message Deleted:"+ queueMessageItem.getMessageId()))
         //    .subscribe(throwable -> System.out.println("Got error: " + throwable.getMessageText()));
    }

    private static Flux<Image> getImage (Flux<QueueMessageItem> itemFlux) {
        return itemFlux.map(queueMessageItem -> new Gson().fromJson(queueMessageItem.getMessageText(), Image.class));
    }

    private static Flux<Image> getFormPages(Flux<Image> imageFlux) {
        Function<List<FormPage>, String> getText = formPages ->
                formPages.stream()
                        .flatMap(formPage -> formPage.getLines().stream())
                        .map(FormLine::getText)
                        .collect(Collectors.joining());

        return imageFlux.flatMap(image ->
            data.formRecognizerAsyncClient.beginRecognizeContentFromUrl(image.getBlobUri())
                    .flatMap(AsyncPollResponse::getFinalResult)
                    .map(getText)
                    .map(s -> {
                       image.setText(s);
                       return image;
                    })
        );
    }

    private static Flux<Image> analyzeTextSentiment(Flux<Image> imageFlux){
        return imageFlux.flatMap(image ->
                data.textAnalyticsAsyncClient.analyzeSentiment(image.getText())
                   .map(sentiment -> {
                       image.setSentiment(sentiment.getSentiment().toString());
                       return image;
                   }));
    }

    private static Flux<CosmosItemResponse<Image>> writeToCosmos (Flux<Image> imageFlux){
        return imageFlux.flatMap(image ->
            data.cosmosAsyncContainer.upsertItem(image)
                    .doOnNext(imageCosmosItemResponse -> System.out.println(imageCosmosItemResponse.getActivityId())));
    }
}