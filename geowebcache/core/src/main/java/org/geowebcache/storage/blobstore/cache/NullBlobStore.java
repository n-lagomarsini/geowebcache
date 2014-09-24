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
 */
package org.geowebcache.storage.blobstore.cache;

import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;

public class NullBlobStore implements BlobStore {

    @Override
    public boolean delete(String layerName) throws StorageException {
        return true;
    }

    @Override
    public boolean deleteByGridsetId(String layerName, String gridSetId) throws StorageException {
        return true;
    }

    @Override
    public boolean delete(TileObject obj) throws StorageException {
        return true;
    }

    @Override
    public boolean delete(TileRange obj) throws StorageException {
        return true;
    }

    @Override
    public boolean get(TileObject obj) throws StorageException {
        return false;
    }

    @Override
    public void put(TileObject obj) throws StorageException {
    }

    @Override
    public void clear() throws StorageException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void addListener(BlobStoreListener listener) {
    }

    @Override
    public boolean removeListener(BlobStoreListener listener) {
        return true;
    }

    @Override
    public boolean rename(String oldLayerName, String newLayerName) throws StorageException {
        return true;
    }

    @Override
    public String getLayerMetadata(String layerName, String key) {
        return null;
    }

    @Override
    public void putLayerMetadata(String layerName, String key, String value) {
    }

}
