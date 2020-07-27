package azsdkdemo.java.lib;

import com.azure.ai.formrecognizer.FormRecognizerAsyncClient;
import com.azure.ai.formrecognizer.FormRecognizerClientBuilder;
import com.azure.ai.textanalytics.TextAnalyticsAsyncClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.identity.*;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueAsyncClient;
import com.azure.storage.queue.QueueServiceAsyncClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import reactor.core.publisher.Mono;

public class DataAsync {

    public Dotenv dotEnv = Dotenv.configure().directory("/Users/heba-mac/git-repo/Java-SDK").load();

    private ChainedTokenCredential chainedTokenCredential;

    public CosmosAsyncClient cosmosAsyncClient;
    public CosmosAsyncContainer cosmosAsyncContainer;
    public SecretAsyncClient secretAsyncClient;
    public BlobContainerAsyncClient blobContainerAsyncClient;
    public BlobServiceAsyncClient blobServiceAsyncClient;
    public QueueServiceAsyncClient queueServiceAsyncClient;
    public QueueAsyncClient queueAsyncClient;
    public TextAnalyticsAsyncClient textAnalyticsAsyncClient;
    public FormRecognizerAsyncClient formRecognizerAsyncClient;

    public void initializeClients(){

        this.chainedTokenCredential = getCredentialChain();

        keyVaultSecretsClient();
        cosmosClient();
        cosmosContainer(this.cosmosAsyncClient, dotEnv.get("AZURE_COSMOS_DB"), dotEnv.get("AZURE_COSMOS_CONTAINER"));

        blobServiceClient(dotEnv.get("AZURE_STORAGE_BLOB_ENDPOINT"));
        this.blobContainerAsyncClient = this.blobServiceAsyncClient
                .getBlobContainerAsyncClient(dotEnv.get("AZURE_STORAGE_BLOB_CONTAINER_NAME"));

        queueServiceClient(dotEnv.get("AZURE_STORAGE_QUEUE_ENDPOINT"));
        this.queueAsyncClient = this.queueServiceAsyncClient.getQueueAsyncClient(dotEnv.get("AZURE_STORAGE_QUEUE_NAME"));

        formRecognizerClient(dotEnv.get("AZURE_FORM_RECOGNIZER_ENDPOINT"));
        textAnalyticsClient(dotEnv.get("AZURE_TEXT_ANALYTICS_ENDPOINT"));
    }

    private ChainedTokenCredential getCredentialChain() {

        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .clientId(dotEnv.get("AZURE_CLIENT_ID"))
                .build();

        ClientSecretCredential secondServicePrincipal = new ClientSecretCredentialBuilder()
                .clientId(dotEnv.get("AZURE_CLIENT_ID"))
                .clientSecret(dotEnv.get("AZURE_CLIENT_SECRET"))
                .tenantId(dotEnv.get("AZURE_TENANT_ID"))
                .build();

        return new ChainedTokenCredentialBuilder()
                .addFirst(managedIdentityCredential)
                .addLast(secondServicePrincipal)
                .build();
    }

    private void keyVaultSecretsClient() {

        this.secretAsyncClient =  new SecretClientBuilder()
                .credential(this.chainedTokenCredential)
                .vaultUrl(dotEnv.get("AZURE_KEYVAULT_ENDPOINT"))
                .buildAsyncClient();
    }

    private void cosmosClient() {
        Mono<KeyVaultSecret> cosmosKey = this.secretAsyncClient.getSecret(dotEnv.get("AZURE_COSMOS_KEY_NAME"));
        this.cosmosAsyncClient =  new CosmosClientBuilder()
                .endpoint(dotEnv.get("AZURE_COSMOS_ENDPOINT"))
                .key(cosmosKey.block().getValue()).buildAsyncClient();
    }

    private void cosmosContainer(
            final CosmosAsyncClient cosmosClient,
            final String dbName,
            final String containerName) {
        this.cosmosAsyncContainer =  cosmosClient.getDatabase(dbName).getContainer(containerName);
    }

    private void blobServiceClient(
            final String storageEndpoint) {
        this.blobServiceAsyncClient = new BlobServiceClientBuilder()
                .endpoint(storageEndpoint)
                .buildAsyncClient();
    }

    private void queueServiceClient (
            final String storageQueueEndpoint) {
        this.queueServiceAsyncClient = new QueueServiceClientBuilder()
                .endpoint(storageQueueEndpoint)
                .credential(this.chainedTokenCredential)
                .buildAsyncClient();
    }

    private void formRecognizerClient(
            final String recognizerEndpoint) {
        this.formRecognizerAsyncClient =  new FormRecognizerClientBuilder()
                .endpoint(recognizerEndpoint)
                .credential(this.chainedTokenCredential)
                .buildAsyncClient();
    }

    private void textAnalyticsClient(
            final String textAnalyzerEndpoint) {
        this.textAnalyticsAsyncClient =  new TextAnalyticsClientBuilder()
                .endpoint(textAnalyzerEndpoint)
                .credential(this.chainedTokenCredential)
                .buildAsyncClient();
    }

}
