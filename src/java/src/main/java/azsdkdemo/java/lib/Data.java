package azsdkdemo.java.lib;

import com.azure.ai.formrecognizer.FormRecognizerClient;
import com.azure.ai.formrecognizer.FormRecognizerClientBuilder;
import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.identity.*;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import io.github.cdimascio.dotenv.Dotenv;

public class Data {

    public Dotenv dotEnv = Dotenv.configure().directory("/Users/heba-mac/git-repo/Java-SDK").load();

    private ChainedTokenCredential chainedTokenCredential;

    public CosmosClient cosmosClient;
    public CosmosContainer cosmosContainer;
    public SecretClient secretClient;
    public BlobContainerClient blobContainerClient;
    public BlobServiceClient blobServiceClient;
    public QueueServiceClient queueServiceClient;
    public QueueClient queueClient;
    public TextAnalyticsClient textAnalyticsClient;
    public FormRecognizerClient formRecognizerClient;

    public void initializeClients(){

        this.chainedTokenCredential = getCredentialChain();

        keyVaultSecretsClient();
        cosmosClient();
        cosmosContainer(this.cosmosClient, dotEnv.get("AZURE_COSMOS_DB"), dotEnv.get("AZURE_COSMOS_CONTAINER"));

        blobServiceClient(dotEnv.get("AZURE_STORAGE_BLOB_ENDPOINT"));
        this.blobContainerClient = this.blobServiceClient
                .getBlobContainerClient(dotEnv.get("AZURE_STORAGE_BLOB_CONTAINER_NAME"));

        queueServiceClient(dotEnv.get("AZURE_STORAGE_QUEUE_ENDPOINT"));
        this.queueClient = this.queueServiceClient.getQueueClient(dotEnv.get("AZURE_STORAGE_QUEUE_NAME"));

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

        this.secretClient =  new SecretClientBuilder()
                .credential(this.chainedTokenCredential)
                .vaultUrl(dotEnv.get("AZURE_KEYVAULT_ENDPOINT"))
                .buildClient();
    }

    private void cosmosClient() {
        KeyVaultSecret cosmosKey = this.secretClient.getSecret(dotEnv.get("AZURE_COSMOS_KEY_NAME"));
        this.cosmosClient =  new CosmosClientBuilder()
                .endpoint(dotEnv.get("AZURE_COSMOS_ENDPOINT"))
                .key(cosmosKey.getValue()).buildClient();
    }

    private void cosmosContainer(
            final CosmosClient cosmosClient,
            final String dbName,
            final String containerName) {
        this.cosmosContainer =  cosmosClient.getDatabase(dbName).getContainer(containerName);
    }

    private void blobServiceClient(
            final String storageEndpoint) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(storageEndpoint)
                .buildClient();
    }

    private void queueServiceClient (
            final String storageQueueEndpoint) {
        this.queueServiceClient = new QueueServiceClientBuilder()
                .endpoint(storageQueueEndpoint)
                .credential(this.chainedTokenCredential)
                .buildClient();
    }

    private void formRecognizerClient(
            final String recognizerEndpoint) {
        this.formRecognizerClient =  new FormRecognizerClientBuilder()
                .endpoint(recognizerEndpoint)
                .credential(this.chainedTokenCredential)
                .buildClient();
    }

    private void textAnalyticsClient(
            final String textAnalyzerEndpoint) {
        this.textAnalyticsClient =  new TextAnalyticsClientBuilder()
                .endpoint(textAnalyzerEndpoint)
                .credential(this.chainedTokenCredential)
                .buildClient();
    }

}
