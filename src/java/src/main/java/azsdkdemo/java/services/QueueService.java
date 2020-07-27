package azsdkdemo.java.services;

import azsdkdemo.java.lib.Data;
import azsdkdemo.java.lib.Image;
import com.azure.ai.formrecognizer.models.FormPage;
import com.azure.ai.formrecognizer.models.OperationResult;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.queue.models.QueueMessageItem;
import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

public class QueueService {

    public static Dotenv dotEnv = Dotenv.configure().directory("/Users/heba-mac/git-repo/Java-SDK").load();
    private static Data data = new Data();


    public static void main(String[] args) throws InterruptedException, URISyntaxException {
        data.initializeClients();

        System.out.println("Receiving Messages...");
        PagedIterable<QueueMessageItem> messages = data.queueClient.receiveMessages(Integer.valueOf(dotEnv.get("AZURE_STORAGE_QUEUE_MSG_COUNT")));

        for (QueueMessageItem message: messages) {
            System.out.println(message.getMessageText());
            Image image = new Gson().fromJson(message.getMessageText(), Image.class);

            SyncPoller<OperationResult, List<FormPage>> recognizeContentOperation =
                    data.formRecognizerClient.beginRecognizeContentFromUrl(new URI(image.getBlobUri()).toString());

            PollResponse recognizeContentCompletion = recognizeContentOperation.waitForCompletion();
            Object content = recognizeContentCompletion.getValue();
            //Stream stream = content.flatMap(List::stream);
        }
    }
}
