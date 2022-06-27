/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.shared.views

import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * A Material [ListItem] displaying a dropdown menu to select a value. The current value is
 * displayed on the right.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> SelectorListItem(
    label: String,
    values: List<T>,
    selection: T,
    titleOfSelection: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
) {
    var isExpanded by remember { mutableStateOf(false) }
    fun dismiss() { isExpanded = false }

    ListItem(
        modifier = Modifier.run {
            if (enabled) clickable { isExpanded = true }
            else this
        },
        text = {
            Group(enabled = enabled) {
                Text(label)
            }
        },
        trailing = {
            Group(enabled = enabled) {
                Text(titleOfSelection(selection))
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { dismiss() }
            ) {
                for (value in values) {
                    DropdownMenuItem(
                        onClick = {
                            onSelected(value)
                            dismiss()
                        }
                    ) {
                        Text(titleOfSelection(value))
                    }
                }
            }
        }
    )
}