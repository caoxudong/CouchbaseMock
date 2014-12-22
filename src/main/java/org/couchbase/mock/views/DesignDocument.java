/**
 *     Copyright 2012 Couchbase, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.couchbase.mock.views;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.couchbase.mock.JsonUtils;

/**
 *
 * @author Sergey Avseyev
 */
public class DesignDocument {

    final private String body;
    private String id;
    final private ArrayList<View> views;

    private DesignDocument(String body) {
        this.body = body;
        this.views = new ArrayList<View>();
    }

    public static DesignDocument create(String body, String name) throws DesignParseException {
        DesignDocument doc = new DesignDocument(body);
        doc.id = "_design/" + name;
        doc.load();
        return doc;
    }

    private void load() throws DesignParseException {
        try {
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            if (obj.has("_id")) {
                this.id = obj.get("_id").getAsString();
            }
            if (this.id == null) {
                throw new DesignParseException("No _id specified and no implicit path provided");
            }

            JsonObject viewsJson = obj.getAsJsonObject("views");
            if (viewsJson == null) {
                throw new DesignParseException("Missing `views`");
            }

            for (Map.Entry<String,JsonElement> entry  : viewsJson.entrySet()) {
                JsonElement curElem = entry.getValue();
                if (!curElem.isJsonObject()) {
                    throw new DesignParseException("View contents must be a JSON dictionary");
                }

                JsonObject view = curElem.getAsJsonObject();
                if (!view.has("map")) {
                    throw new DesignParseException("Missing `map` function");
                }

                String mapSrc = view.get("map").getAsString();
                String reduceSrc = null;
                if (view.has("reduce")) {
                    reduceSrc = view.get("reduce").getAsString();
                }
                views.add(new View(entry.getKey(), mapSrc, reduceSrc));
            }
        } catch (ScriptException ex) {
            throw new DesignParseException(ex);
        } catch (JsonParseException ex) {
            throw new DesignParseException(ex);
        }
    }

    public String getBody() {
        return body;
    }

    public String getId() {
        return id;
    }

    public ArrayList<View> getViews() {
        return views;
    }
}
