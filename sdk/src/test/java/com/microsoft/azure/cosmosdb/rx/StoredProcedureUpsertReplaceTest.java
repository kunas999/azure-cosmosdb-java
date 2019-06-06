/*
 * The MIT License (MIT)
 * Copyright (c) 2018 Microsoft Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.cosmosdb.rx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.microsoft.azure.cosmosdb.internal.directconnectivity.Protocol;

import reactor.core.publisher.Mono;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.microsoft.azure.cosmos.CosmosClient;
import com.microsoft.azure.cosmos.CosmosContainer;
import com.microsoft.azure.cosmos.CosmosResponseValidator;
import com.microsoft.azure.cosmos.CosmosStoredProcedure;
import com.microsoft.azure.cosmos.CosmosStoredProcedureRequestOptions;
import com.microsoft.azure.cosmos.CosmosStoredProcedureResponse;
import com.microsoft.azure.cosmos.CosmosStoredProcedureSettings;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.cosmosdb.RequestOptions;

public class StoredProcedureUpsertReplaceTest extends TestSuiteBase {

    private CosmosContainer createdCollection;

    private CosmosClient client;

    @Factory(dataProvider = "clientBuildersWithDirect")
    public StoredProcedureUpsertReplaceTest(CosmosClient.Builder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void replaceStoredProcedure() throws Exception {

        // create a stored procedure
        CosmosStoredProcedureSettings storedProcedureDef = new CosmosStoredProcedureSettings();
        storedProcedureDef.setId(UUID.randomUUID().toString());
        storedProcedureDef.setBody("function() {var x = 10;}");
        CosmosStoredProcedureSettings readBackSp = createdCollection.createStoredProcedure(storedProcedureDef, new CosmosStoredProcedureRequestOptions()).block().getStoredProcedureSettings();

        // read stored procedure to validate creation
        waitIfNeededForReplicasToCatchUp(clientBuilder);
        Mono<CosmosStoredProcedureResponse> readObservable = createdCollection.getStoredProcedure(readBackSp.getId()).read(null);

        // validate stored procedure creation
        CosmosResponseValidator<CosmosStoredProcedureResponse> validatorForRead = new CosmosResponseValidator.Builder<CosmosStoredProcedureResponse>()
                .withId(readBackSp.getId())
                .withStoredProcedureBody("function() {var x = 10;}")
                .notNullEtag()
                .build();
        validateSuccess(readObservable, validatorForRead);
        
        //update stored procedure
        readBackSp.setBody("function() {var x = 11;}");

        Mono<CosmosStoredProcedureResponse> replaceObservable = createdCollection.getStoredProcedure(readBackSp.getId()).replace(readBackSp, new RequestOptions());

        //validate stored procedure replace
        CosmosResponseValidator<CosmosStoredProcedureResponse> validatorForReplace = new CosmosResponseValidator.Builder<CosmosStoredProcedureResponse>()
                .withId(readBackSp.getId())
                .withStoredProcedureBody("function() {var x = 11;}")
                .notNullEtag()
                .build();
        validateSuccess(replaceObservable, validatorForReplace);   
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void executeStoredProcedure() throws Exception {
        // create a stored procedure
        CosmosStoredProcedureSettings storedProcedureDef = new CosmosStoredProcedureSettings(
                "{" +
                        "  'id': '" +UUID.randomUUID().toString() + "'," +
                        "  'body':" +
                        "    'function () {" +
                        "      for (var i = 0; i < 10; i++) {" +
                        "        getContext().getResponse().appendValue(\"Body\", i);" +
                        "      }" +
                        "    }'" +
                        "}");

        CosmosStoredProcedure storedProcedure = null;

        try {
            storedProcedure = createdCollection.createStoredProcedure(storedProcedureDef, new CosmosStoredProcedureRequestOptions()).block().getStoredProcedure();
        } catch (Throwable error) {
            if (this.clientBuilder.getConfigs().getProtocol() == Protocol.Tcp) {
                String message = String.format("Direct TCP test failure ignored: desiredConsistencyLevel=%s", this.clientBuilder.getDesiredConsistencyLevel());
                logger.info(message, error);
                throw new SkipException(message, error);
            }
            throw error;
        }

        String result = null;

        try {
            RequestOptions options = new RequestOptions();
            options.setPartitionKey(PartitionKey.None);
            result = storedProcedure.execute(null, options).block().getResponseAsString();
        } catch (Throwable error) {
            if (this.clientBuilder.getConfigs().getProtocol() == Protocol.Tcp) {
                String message = String.format("Direct TCP test failure ignored: desiredConsistencyLevel=%s", this.clientBuilder.getDesiredConsistencyLevel());
                logger.info(message, error);
                throw new SkipException(message, error);
            }
            throw error;
        }

        assertThat(result).isEqualTo("\"0123456789\"");
    }

    @BeforeClass(groups = { "simple" }, timeOut = SETUP_TIMEOUT)
    public void beforeClass() {
        client = clientBuilder.build();
        createdCollection = getSharedMultiPartitionCosmosContainer(client);
    }

    @AfterClass(groups = { "simple" }, timeOut = SHUTDOWN_TIMEOUT, alwaysRun = true)
    public void afterClass() {
        safeClose(client);
    }
}
