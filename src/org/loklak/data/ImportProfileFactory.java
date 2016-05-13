/**
 *  ImportProfileFactory
 *  Copyright 24.07.2015 by Dang Hai An, @zyzo
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package org.loklak.data;

import org.json.JSONObject;
import org.loklak.objects.ImportProfileEntry;

import java.io.IOException;

public class ImportProfileFactory extends AbstractIndexFactory<ImportProfileEntry> implements IndexFactory<ImportProfileEntry> {

    public ImportProfileFactory(ElasticsearchClient elasticsearch_client, String index_name, int cacheSize, final int existSize) {
        super(elasticsearch_client, index_name, cacheSize, existSize);
    }

    @Override
    public ImportProfileEntry init(JSONObject map) throws IOException {
        return new ImportProfileEntry(map);
    }
}
