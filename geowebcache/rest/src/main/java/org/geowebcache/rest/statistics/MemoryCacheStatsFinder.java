/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.geowebcache.rest.statistics;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.StorageBroker;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class MemoryCacheStatsFinder extends Finder {

    public static Log LOG = LogFactory.getLog(MemoryCacheStatsFinder.class);

    private BlobStore store;

    public MemoryCacheStatsFinder(StorageBroker broker) {
        super(null, MemoryCacheStatsResource.class);

        BlobStore privateStore = null;
        try {
            Field privateblobStore = broker.getClass().getDeclaredField("blobStore");

            privateblobStore.setAccessible(true);

            privateStore = (BlobStore) privateblobStore.get(broker);
        } catch (NoSuchFieldException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (SecurityException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (IllegalArgumentException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (IllegalAccessException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }

        this.store = privateStore;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        MemoryCacheStatsResource resource = (MemoryCacheStatsResource) super.findTarget(request,
                response);
        resource.setBlobStore(store);
        return resource;
    }
}
