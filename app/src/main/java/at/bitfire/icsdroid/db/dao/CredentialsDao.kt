/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import at.bitfire.icsdroid.db.entity.Credential

@Dao
interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE subscriptionId=:subscriptionId")
    fun getBySubscriptionId(subscriptionId: Long): Credential?

    @Insert
    fun create(credential: Credential)

    @Upsert
    fun upsert(credential: Credential)

    @Query("DELETE FROM credentials WHERE subscriptionId=:subscriptionId")
    fun removeBySubscriptionId(subscriptionId: Long)

    @Update
    fun update(credential: Credential)

}
