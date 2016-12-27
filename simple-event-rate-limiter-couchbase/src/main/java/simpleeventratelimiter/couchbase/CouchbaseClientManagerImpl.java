/**
 * Copyright © 2016 Klemen Polanec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package simpleeventratelimiter.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Couchbase manager implementation
 *
 * Created by Klemen Polanec on 27.12.2016.
 */
public class CouchbaseClientManagerImpl implements CouchbaseClientManager {
    private static final Logger log = LoggerFactory.getLogger(CouchbaseClientManagerImpl.class);

    private static final String DEFAULT_PROPERTIES_RESOURCE_FILENAME = "couchbase_limiter_default.properties";
    private static final String PROPERTIES_RESOURCE_FILENAME = "couchbase_limiter.properties";

    private static final String BUCKET_PASSWORD_PROPERTIY_KEY = "bucket.password";
    private static final String BUCKET_NAME_PROPERTIY_KEY = "bucket.name";
    private static final String COUCHBASE_HOSTS_PROPERTIY_KEY = "couchbase.hosts";

    private Bucket client = null;
    private Cluster cluster;

    private Properties properties;

    private static boolean clientInRemoval = false;

    private static CouchbaseClientManagerImpl instance;

    private CouchbaseClientManagerImpl()
    {
        super();
        properties = new Properties();
        try {
            InputStream is = this.getClass().getResourceAsStream(PROPERTIES_RESOURCE_FILENAME);
            if (is==null)
            {
                log.info("No resource file " + PROPERTIES_RESOURCE_FILENAME + ". Loading from " + DEFAULT_PROPERTIES_RESOURCE_FILENAME + ".");
                is = this.getClass().getResourceAsStream(DEFAULT_PROPERTIES_RESOURCE_FILENAME);
            }
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CouchbaseClientManagerImpl getInstance()
    {
        if (instance == null)
        {
            return getInstance();
        }
        else {
            return instance;
        }
    }

    private String getBucketName()
    {
        return properties.getProperty(BUCKET_NAME_PROPERTIY_KEY);
    }

    private String getBucketPassword()
    {
        return properties.getProperty(BUCKET_PASSWORD_PROPERTIY_KEY);
    }

    private List<String> getHosts()
    {
        String hostsString  = properties.getProperty(COUCHBASE_HOSTS_PROPERTIY_KEY);
        List<String> hosts = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(hostsString, ",");
        while (st.hasMoreElements()) {
            String stringValue = st.nextToken();
            stringValue = stringValue.trim();
            hosts.add(stringValue);
        }
        return hosts;
    }

    @Override
    public Bucket getClient() throws Exception {
        if (client!=null) {
            return client;
        }

        log.debug("Creating couchbase client: " + getBucketName());
        synchronized (this.getClass()) {
            if (client!=null) {
                return client;
            }
            try {
                Cluster cluster = getCluster();

                client = cluster.openBucket(getBucketName(), getBucketPassword(),20, TimeUnit.SECONDS);
                log.info("Couchbase client created: " + getBucketName());
                return client;
            } catch (Exception e) {
                log.error("Could not create couchbase client: " + getBucketName() + " error:" + e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public void removeClient() {
        log.info("Removing client for bucket " + getBucketName());
        if (clientInRemoval) {
            return;
        }
        synchronized (this.getClass()) {
            if (clientInRemoval) {
                return;
            }
            try {
                clientInRemoval = true;
                try {
                    Bucket client = null;
                    if (client != null) {
                        log.debug("Closing client for bucket " + getBucketName());
                        client.close(20, TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                }
            } finally {
                clientInRemoval = false;
            }
        }
    }


    @Override
    public void closeConnections() {
        log.info("Closing couchbase cluster connections ...");

        // Shutdown clients
        try {
            //log.info("Shutting down couchbase client: " + key);
            if (client != null) {
                client.close(20, TimeUnit.SECONDS);
            }
            client = null;
        } catch (Exception e) {
            log.warn("Error shutting down couchbase client: " + getBucketName() + " msg:" + e.getMessage());
            e.printStackTrace();
        }


        if (cluster != null) {
            cluster.disconnect(20, TimeUnit.SECONDS);
            cluster = null;
        }
        log.info("Couchbase cluster connections closed");
    }

    @Override
    protected void finalize() throws Throwable {
        closeConnections();
        super.finalize();
    }

    private Cluster getCluster() {
        if (cluster != null) {
            return cluster;
        } else {
            cluster = CouchbaseCluster.create(getHosts());
        }
        return cluster;
    }

}
