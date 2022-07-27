/* Copyright 2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nicindustries.hibernate.memcached.spymemcached;

import com.nicindustries.hibernate.memcached.Config;
import com.nicindustries.hibernate.memcached.Memcache;
import com.nicindustries.hibernate.memcached.MemcacheClientFactory;
import com.nicindustries.hibernate.memcached.PropertiesHelper;
import net.spy.memcached.*;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

public class SpyMemcacheClientFactory implements MemcacheClientFactory {

    public static final String PROP_SERVERS = Config.PROP_PREFIX + "servers";
    public static final String PROP_OPERATION_QUEUE_LENGTH = Config.PROP_PREFIX + "operationQueueLength";
    public static final String PROP_READ_BUFFER_SIZE = Config.PROP_PREFIX + "readBufferSize";
    public static final String PROP_OPERATION_TIMEOUT = Config.PROP_PREFIX + "operationTimeout";
    public static final String PROP_HASH_ALGORITHM = Config.PROP_PREFIX + "hashAlgorithm";
    public static final String PROP_CONNECTION_FACTORY = Config.PROP_PREFIX + "connectionFactory";
    public static final String PROP_DAEMON_MODE = Config.PROP_PREFIX + "daemonMode";
    public static final String PROP_USERNAME = Config.PROP_PREFIX + "username";
    public static final String PROP_PASSWORD = Config.PROP_PREFIX + "password";
    private final PropertiesHelper properties;

    public SpyMemcacheClientFactory(PropertiesHelper properties) {
        this.properties = properties;
    }

    public Memcache createMemcacheClient() throws Exception {

        ConnectionFactory connectionFactory = getConnectionFactory();

        MemcachedClient client = new MemcachedClient(connectionFactory, AddrUtil.getAddresses(getServerList()));
        return new SpyMemcache(client);
    }

    protected ConnectionFactory getConnectionFactory() {

        if (connectionFactoryNameEquals(DefaultConnectionFactory.class)) {
            return buildDefaultConnectionFactory();
        }

        if (connectionFactoryNameEquals(KetamaConnectionFactory.class)) {
            return buildKetamaConnectionFactory();
        }

        if (connectionFactoryNameEquals(BinaryConnectionFactory.class)) {
            return buildBinaryConnectionFactory();
        }

        throw new IllegalArgumentException("Unsupported " + PROP_CONNECTION_FACTORY + " value: " + getConnectionFactoryName());
    }

    private boolean connectionFactoryNameEquals(Class<?> cls) {
        return cls.getSimpleName().equals(getConnectionFactoryName());
    }

    private DefaultConnectionFactory buildDefaultConnectionFactory() {
        return new DefaultConnectionFactory(getOperationQueueLength(), getReadBufferSize(), getHashAlgorithm()) {
            @Override
            public long getOperationTimeout() {
                return getOperationTimeoutMillis();
            }

            @Override
            public boolean isDaemon() {
                return isDaemonMode();
            }

            @Override
            public AuthDescriptor getAuthDescriptor() {
                return createAuthDescriptor();
            }
        };
    }

    private KetamaConnectionFactory buildKetamaConnectionFactory() {
        return new KetamaConnectionFactory() {
            @Override
            public long getOperationTimeout() {
                return getOperationTimeoutMillis();
            }

            @Override
            public boolean isDaemon() {
                return isDaemonMode();
            }

            @Override
            public AuthDescriptor getAuthDescriptor() {
                return createAuthDescriptor();
            }
        };
    }

    private BinaryConnectionFactory buildBinaryConnectionFactory() {
        return new BinaryConnectionFactory(getOperationQueueLength(), getReadBufferSize(), getHashAlgorithm()) {
            @Override
            public long getOperationTimeout() {
                return getOperationTimeoutMillis();
            }

            @Override
            public boolean isDaemon() {
                return isDaemonMode();
            }

            @Override
            public AuthDescriptor getAuthDescriptor() {
                return createAuthDescriptor();
            }
        };
    }

    protected AuthDescriptor createAuthDescriptor() {
        String username = properties.get(PROP_USERNAME);
        String password = properties.get(PROP_PASSWORD);
        if (username == null || password == null) {
            return null;
        }
        return new AuthDescriptor(new String[]{"PLAIN"}, new PlainCallbackHandler(username, password));
    }

    public String getServerList() {
        return properties.get(PROP_SERVERS, "localhost:11211");
    }

    public int getOperationQueueLength() {
        return properties.getInt(PROP_OPERATION_QUEUE_LENGTH, DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN);
    }

    public int getReadBufferSize() {
        return properties.getInt(PROP_READ_BUFFER_SIZE, DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE);
    }

    public long getOperationTimeoutMillis() {
        return properties.getLong(PROP_OPERATION_TIMEOUT, DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT);
    }

    public boolean isDaemonMode() {
        return properties.getBoolean(PROP_DAEMON_MODE, false);
    }

    public HashAlgorithm getHashAlgorithm() {
        return properties.getEnum(PROP_HASH_ALGORITHM, DefaultHashAlgorithm.class, DefaultHashAlgorithm.NATIVE_HASH);
    }

    public String getConnectionFactoryName() {
        return properties.get(PROP_CONNECTION_FACTORY, DefaultConnectionFactory.class.getSimpleName());
    }

    protected PropertiesHelper getProperties() {
        return properties;
    }
}
