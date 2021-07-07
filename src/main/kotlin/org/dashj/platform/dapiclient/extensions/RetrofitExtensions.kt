/**
 * Copyright (c) 2018-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.extensions

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Extension function to modify retrofit callback to use lambdas
 */
fun <T> Call<T>.enqueue(
    success: (response: Response<T>) -> Unit,
    failure: (t: Throwable) -> Unit
) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>?, response: Response<T>) = success(response)

        override fun onFailure(call: Call<T>?, t: Throwable) = failure(t)
    })
}
