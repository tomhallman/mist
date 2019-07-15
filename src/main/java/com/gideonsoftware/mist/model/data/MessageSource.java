/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Gideon Software
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, visit https://www.gideonsoftware.com
 */

package com.gideonsoftware.mist.model.data;

import java.time.LocalDateTime;

/**
 * Base class for message sources.
 */
public class MessageSource {
    // private static Logger log = LogManager.getLogger();

    private Integer sourceId = 0;
    private String sourceName = "";
    private LocalDateTime date = LocalDateTime.now();
    private String fromId = "";
    private String fromName = "";
    private Object[] recipients = new Object[0];
    private String subject = "";
    private String body = "";

    /**
     * Whether to add "existing" history into the model during processing.
     * <p>
     * If false, new history, history with unknown contacts, and error state history, will still be added.
     */
    private boolean addExistingHistory = true;

    public MessageSource() {
    }

    public MessageSource(MessageSource messageSource) {
        this.sourceId = messageSource.sourceId;
        this.sourceName = messageSource.sourceName;
        this.date = messageSource.date;
        this.fromId = messageSource.fromId;
        this.fromName = messageSource.fromName;
        this.recipients = messageSource.recipients.clone();
        this.subject = messageSource.subject;
        this.body = messageSource.body;
        this.addExistingHistory = messageSource.addExistingHistory;
    }

    /**
     * Returns a clone of this message source.
     * 
     * @return a clone of this message source
     * @see https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
     */
    public MessageSource cloneObject() {
        return new MessageSource(this);
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getFromId() {
        return fromId;
    }

    public String getFromName() {
        return fromName;
    }

    public Object[] getRecipients() {
        return recipients;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isAddExistingHistory() {
        return addExistingHistory;
    }

    public void setAddExistingHistory(boolean addExistingHistory) {
        this.addExistingHistory = addExistingHistory;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setRecipients(Object[] recipients) {
        this.recipients = recipients;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String toString() {
        return String.format(
            "from:%s|%s; date:%s; source:%s|%s%s",
            fromId,
            fromName,
            date,
            sourceId,
            sourceName,
            addExistingHistory ? "" : "; ** addExistingHistory=false **");
    }
}