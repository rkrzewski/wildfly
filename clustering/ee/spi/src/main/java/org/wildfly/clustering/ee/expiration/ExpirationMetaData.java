/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.ee.expiration;

import java.time.Instant;

/**
 * Describes expiration-related metadata.
 * @author Paul Ferraro
 */
public interface ExpirationMetaData extends Expiration {

    /**
     * Indicates whether or not this object is expired.
     * @return true, if this object has expired, false otherwise.
     */
    default boolean isExpired() {
        if (this.isImmortal()) return false;
        Instant lastAccessedTime = this.getLastAccessTime();
        return (lastAccessedTime != null) ? lastAccessedTime.plus(this.getTimeout()).isBefore(Instant.now()) : false;
    }

    /**
     * Returns the time this object was last accessed.
     * @return the time this object was last accessed.
     */
    Instant getLastAccessTime();
}
