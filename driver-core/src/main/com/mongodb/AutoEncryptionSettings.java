/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb;

import com.mongodb.annotations.NotThreadSafe;
import com.mongodb.lang.Nullable;
import org.bson.BsonDocument;

import java.util.Collections;
import java.util.Map;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * The client-side automatic encryption settings. Client side encryption enables an application to specify what fields in a collection
 * must be encrypted, and the driver automatically encrypts commands sent to MongoDB and decrypts responses.
 * <p>
 * Automatic encryption is an enterprise only feature that only applies to operations on a collection. Automatic encryption is not
 * supported for operations on a database or view and will result in error. To bypass automatic encryption,
 * set bypassAutoEncryption=true in {@code AutoEncryptionSettings}.
 * </p>
 * <p>
 * Explicit encryption/decryption and automatic decryption is a community feature, enabled with the new
 * {@code com.mongodb.client.vault.ClientEncryption} type.
 * </p>
 * <p>
 * A MongoClient configured with bypassAutoEncryption=true will still automatically decrypt.
 * </p>
 * <p>
 * If automatic encryption fails on an operation, use a MongoClient configured with bypassAutoEncryption=true and use
 * ClientEncryption#encrypt to manually encrypt values.
 * </p>
 * <p>
 * Enabling client side encryption reduces the maximum document and message size (using a maxBsonObjectSize of 2MiB and
 * maxMessageSizeBytes of 6MB) and may have a negative performance impact.
 * </p>
 * <p>
 * Automatic encryption requires the authenticated user to have the listCollections privilege action.
 * </p>
 *
 * @since 3.11
 */
public final class AutoEncryptionSettings {
    private final MongoClientSettings keyVaultMongoClientSettings;
    private final String keyVaultNamespace;
    private final Map<String, Map<String, Object>> kmsProviders;
    private final Map<String, BsonDocument> schemaMap;
    private final Map<String, Object> extraOptions;
    private final boolean bypassAutoEncryption;

    /**
     * A builder for {@code AutoEncryptionSettings} so that {@code AutoEncryptionSettings} can be immutable, and to support easier
     * construction through chaining.
     */
    @NotThreadSafe
    public static final class Builder {
        private MongoClientSettings keyVaultMongoClientSettings;
        private String keyVaultNamespace;
        private Map<String, Map<String, Object>> kmsProviders;
        private Map<String, BsonDocument> schemaMap = Collections.emptyMap();
        private Map<String, Object> extraOptions = Collections.emptyMap();
        private boolean bypassAutoEncryption;

        /**
         * Sets the key vault settings.
         *
         * @param keyVaultMongoClientSettings the key vault mongo client settings, which may be null.
         * @return this
         * @see #getKeyVaultMongoClientSettings()
         */
        public Builder keyVaultMongoClientSettings(final MongoClientSettings keyVaultMongoClientSettings) {
            this.keyVaultMongoClientSettings = keyVaultMongoClientSettings;
            return this;
        }

        /**
         * Sets the key vault namespace
         *
         * @param keyVaultNamespace the key vault namespace, which may not be null
         * @return this
         * @see #getKeyVaultNamespace()
         */
        public Builder keyVaultNamespace(final String keyVaultNamespace) {
            this.keyVaultNamespace = notNull("keyVaultNamespace", keyVaultNamespace);
            return this;
        }

        /**
         * Sets the KMS providers map.
         *
         * @param kmsProviders the KMS providers map, which may not be null
         * @return this
         * @see #getKmsProviders()
         */
        public Builder kmsProviders(final Map<String, Map<String, Object>> kmsProviders) {
            this.kmsProviders = notNull("kmsProviders", kmsProviders);
            return this;
        }

        /**
         * Sets the map from namespace to local schema document
         *
         * @param schemaMap the map from namespace to local schema document
         * @return this
         * @see #getSchemaMap()
         */
        public Builder schemaMap(final Map<String, BsonDocument> schemaMap) {
            this.schemaMap = notNull("schemaMap", schemaMap);
            return this;
        }

        /**
         * Sets the extra options.
         *
         * @param extraOptions the extra options, which may not be null
         * @return this
         * @see #getExtraOptions()
         */
        public Builder extraOptions(final Map<String, Object> extraOptions) {
            this.extraOptions = notNull("extraOptions", extraOptions);
            return this;
        }

        /**
         * Sets whether auto-encryption should be bypassed.
         *
         * @param bypassAutoEncryption whether auto-encryption should be bypassed
         * @return this
         * @see #isBypassAutoEncryption()
         */
        public Builder bypassAutoEncryption(final boolean bypassAutoEncryption) {
            this.bypassAutoEncryption = bypassAutoEncryption;
            return this;
        }

        /**
         * Build an instance of {@code AutoEncryptionSettings}.
         *
         * @return the settings from this builder
         */
        public AutoEncryptionSettings build() {
            return new AutoEncryptionSettings(this);
        }

        private Builder() {
        }
    }

    /**
     * Convenience method to create a Builder.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the key vault settings.
     *
     * <p>
     * The key vault collection is assumed to reside on the same MongoDB cluster as the encrypted collections. But the optional
     * keyVaultMongoClientSettings can be used to route data key queries to a separate MongoDB cluster, or the same cluster but using a
     * different credential.
     * </p>
     *
     * @return the key vault settings, which may be null to indicate that the same {@code MongoClient} should be used to access the key
     * vault collection as is used for the rest of the application.
     */
    @Nullable
    public MongoClientSettings getKeyVaultMongoClientSettings() {
        return keyVaultMongoClientSettings;
    }

    /**
     * Gets the key vault namespace.
     *
     * <p>
     * The key vault namespace refers to a collection that contains all data keys used for encryption and decryption (aka the key vault
     * collection). Data keys are stored as documents in a special MongoDB collection. Data keys are protected with encryption by a KMS
     * provider (AWS, Azure, GCP KMS or a local master key).
     * </p>
     *
     * @return the key vault namespace, which may not be null
     */
    public String getKeyVaultNamespace() {
        return keyVaultNamespace;
    }

    /**
     * Gets the map of KMS provider properties.
     *
     * <p>
     * Multiple KMS providers may be specified. The following KMS providers are supported: "aws", "azure", "gcp" and "local". The
     * kmsProviders map values differ by provider:
     * </p>
     * <p>
     * For "aws", the properties are:
     * </p>
     * <ul>
     *     <li>accessKeyId: a String, the AWS access key identifier</li>
     *     <li>secretAccessKey: a String, the AWS secret access key</li>
     *     <li>sessionToken: an optional String, the AWS session token</li>
     * </ul>
     * <p>
     * For "azure", the properties are:
     * </p>
     * <ul>
     *     <li>tenantId: a String, the tenantId that identifies the organization for the account.</li>
     *     <li>clientId: a String, the clientId to authenticate a registered application.</li>
     *     <li>clientSecret: a String, the client secret to authenticate a registered application.</li>
     *     <li>identityPlatformEndpoint: optional String, a host with optional port. e.g. "example.com" or "example.com:443".
     *     Generally used for private Azure instances.</li>
     * </ul>
     * <p>
     * For "gcp", the properties are:
     * </p>
     * <ul>
     *     <li>email: a String, the service account email to authenticate.</li>
     *     <li>privateKey: a String or byte[], the encoded PKCS#8 encrypted key</li>
     *     <li>endPoint: optional String, a host with optional port. e.g. "example.com" or "example.com:443".</li>
     * </ul>
     * <p>
     * For "local", the properties are:
     * </p>
     * <ul>
     *     <li>key: byte[] of length 96, the local key</li>
     * </ul>
     *
     * @return map of KMS provider properties
     */
    public Map<String, Map<String, Object>> getKmsProviders() {
        return kmsProviders;
    }

    /**
     * Gets the map of namespace to local JSON schema.
     * <p>
     * Automatic encryption is configured with an "encrypt" field in a collection's JSONSchema. By default, a collection's JSONSchema is
     * periodically polled with the listCollections command. But a JSONSchema may be specified locally with the schemaMap option.
     * </p>
     * <p>
     * The key into the map is the full namespace of the collection, which is {@code &lt;database name>.&lt;collection name>}.  For
     * example, if the database name is {@code "test"} and the collection name is {@code "users"}, then the namesspace is
     * {@code "test.users"}.
     * </p>
     * <p>
     * Supplying a schemaMap provides more security than relying on JSON Schemas obtained from the server. It protects against a
     * malicious server advertising a false JSON Schema, which could trick the client into sending unencrypted data that should be
     * encrypted.
     * </p>
     * <p>
     * Schemas supplied in the schemaMap only apply to configuring automatic encryption for client side encryption. Other validation
     * rules in the JSON schema will not be enforced by the driver and will result in an error.
     * </p>
     *
     * @return map of namespace to local JSON schema
     */
    public Map<String, BsonDocument> getSchemaMap() {
        return schemaMap;
    }

    /**
     * Gets the extra options that control the behavior of auto-encryption components.
     * <p>
     * The extraOptions currently only relate to the mongocryptd process.  The following options keys are supported:
     * </p>
     * <ul>
     * <li>mongocryptdURI: a String which defaults to "mongodb://%2Fvar%2Fmongocryptd.sock" if domain sockets are available or
     * "mongodb://localhost:27020" otherwise.</li>
     * <li>mongocryptdBypassSpawn: a boolean which defaults to false. If true, the driver will not attempt to automatically spawn a
     * mongocryptd process</li>
     * <li>mongocryptdSpawnPath: specifies the full path to the mongocryptd executable. By default the driver spawns mongocryptd from
     * the system path.</li>
     * <li>mongocryptdSpawnArgs: Used to control the behavior of mongocryptd when the driver spawns it. By default, the driver spawns
     * mongocryptd with the single command line argument {@code "--idleShutdownTimeoutSecs=60"}</li>
     * </ul>
     *
     * @return the extra options map
     */
    public Map<String, Object> getExtraOptions() {
        return extraOptions;
    }

    /**
     * Gets whether auto-encryption should be bypassed.  Even when this option is true, auto-decryption is still enabled.
     * <p>
     * This option is useful for cases where the driver throws an exception because it is unable to prove that the command does not
     * contain any fields that should be automatically encrypted, but the application is able to determine that it does not.  For these
     * cases, the application can construct a {@code MongoClient} with {@code AutoEncryptionSettings} with {@code bypassAutoEncryption}
     * enabled.
     * </p>
     *
     * @return true if auto-encryption should be bypassed
     */
    public boolean isBypassAutoEncryption() {
        return bypassAutoEncryption;
    }

    private AutoEncryptionSettings(final Builder builder) {
        this.keyVaultMongoClientSettings = builder.keyVaultMongoClientSettings;
        this.keyVaultNamespace = notNull("keyVaultNamespace", builder.keyVaultNamespace);
        this.kmsProviders = notNull("kmsProviders", builder.kmsProviders);
        this.schemaMap = notNull("schemaMap", builder.schemaMap);
        this.extraOptions = notNull("extraOptions", builder.extraOptions);
        this.bypassAutoEncryption = builder.bypassAutoEncryption;
    }
}
