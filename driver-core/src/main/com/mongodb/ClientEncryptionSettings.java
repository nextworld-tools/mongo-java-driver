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

import java.util.Map;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * The client-side settings for data key creation and explicit encryption.
 *
 * <p>
 * Explicit encryption/decryption is a community feature, enabled with the new {@code com.mongodb.client.vault.ClientEncryption} type,
 * for which this is the settings.
 * </p>
 *
 * @since 3.11
 */
public final class ClientEncryptionSettings {
    private final MongoClientSettings keyVaultMongoClientSettings;
    private final String keyVaultNamespace;
    private final Map<String, Map<String, Object>> kmsProviders;

    /**
     * A builder for {@code ClientEncryptionSettings} so that {@code ClientEncryptionSettings} can be immutable, and to support easier
     * construction through chaining.
     */
    @NotThreadSafe
    public static final class Builder {
        private MongoClientSettings keyVaultMongoClientSettings;
        private String keyVaultNamespace;
        private Map<String, Map<String, Object>> kmsProviders;

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
         * Build an instance of {@code ClientEncryptionSettings}.
         *
         * @return the settings from this builder
         */
        public ClientEncryptionSettings build() {
            return new ClientEncryptionSettings(this);
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
     * The key vault collection is assumed to reside on the same MongoDB cluster as indicated by the connecting URI. But the optional
     * keyVaultMongoClientSettings can be used to route data key queries to a separate MongoDB cluster, or the same cluster but with a
     * different credential.
     * </p>
     * @return the key vault settings, which may be null to indicate that the same {@code MongoClient} should be used to access the key
     * vault collection as is used for the rest of the application.
     */
    public MongoClientSettings getKeyVaultMongoClientSettings() {
        return keyVaultMongoClientSettings;
    }

    /**
     * Gets the key vault namespace.
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
     *     <li>tenantId: a String, the tenant Id</li>
     *     <li>clientId: a String, the client Id</li>
     *     <li>clientSecret: a String, the clientSecret</li>
     *     <li>identityPlatformEndpoint: an optional String, the identity platform endpoint. Defaults to login.microsoftonline.com</li>
     * </ul>
     * <p>
     * For "gcp", the properties are:
     * </p>
     * <ul>
     *     <li>email: a String, the email address</li>
     *     <li>privateKey: a byte[] or base 64 encoded String, the private key</li>
     *     <li>endpoint: an optional String, the endpoint. Defaults to oauth2.googleapis.com</li>
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

    private ClientEncryptionSettings(final Builder builder) {
        this.keyVaultMongoClientSettings = builder.keyVaultMongoClientSettings;
        this.keyVaultNamespace = notNull("keyVaultNamespace", builder.keyVaultNamespace);
        this.kmsProviders = notNull("kmsProviders", builder.kmsProviders);
    }

}
