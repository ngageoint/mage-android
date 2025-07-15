package mil.nga.giat.mage.ui.login

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.core.graphics.toColorInt
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.ServerAuthTypes


@Composable
fun AuthStrategyButton(
    strategyJson: JSONObject?,
    onClick: () -> Unit,
    defaultButtonColor: Color = colorResource(R.color.md_blue_600),
    defaultContentColor: Color = colorResource(R.color.md_white_1000)
) {

    var buttonText by remember { mutableStateOf("Sign In") }
    var textColor by remember { mutableStateOf(defaultContentColor) }
    var buttonColor by remember { mutableStateOf(defaultButtonColor) }
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(strategyJson) {
        if (strategyJson == null) {
            buttonText = "Sign In"
            textColor = defaultContentColor
            buttonColor = defaultButtonColor
            iconBitmap = null
            return@LaunchedEffect
        }

        val title = strategyJson.optString("title")
        if (title.isNotEmpty()) {
            buttonText = "Sign In With $title"
        }

        textColor = parseColor(strategyJson.optString("textColor"), defaultContentColor)

        val parsedButtonColor = parseColor(strategyJson.optString("buttonColor"), defaultButtonColor)
        buttonColor = parsedButtonColor

        val iconString = strategyJson.optString("icon")
        if (iconString.isNotEmpty()) {
            iconBitmap = decodeBase64Bitmap(iconString)
        } else {
            iconBitmap = null
        }
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap!!.asImageBitmap(),
                        contentDescription = "$buttonText icon",
                        modifier = Modifier.size(26.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "authentication icon",
                        modifier = Modifier.size(18.dp),
                        tint = buttonColor
                    )
                }
            }
        }

        Spacer(Modifier.width(ButtonDefaults.IconSpacing))

        Text(
            text = buttonText,
            style = MaterialTheme.typography.labelMedium
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
}

//parse color, returning a default if parsing fails or color is invalid
fun parseColor(colorString: String?, defaultColor: Color = Color.Unspecified): Color {
    return try {
        if (colorString.isNullOrEmpty()) {
            defaultColor
        } else {
            val parsedColor = colorString.substring(0..6).toColorInt()
            Color(parsedColor or 0xFF000000.toInt())
        }
    } catch (e: Exception) {
        defaultColor
    }
}

//decode Base64 to Bitmap
suspend fun decodeBase64Bitmap(base64String: String?): Bitmap? {
    if (base64String.isNullOrEmpty()) return null
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        withContext(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
    } catch (e: Exception) {
        null
    }
}

@Preview
@Composable
fun AuthButtonPreview() {
    MaterialTheme {
        AuthStrategyButton(
            strategyJson = JSONObject().apply {
                put("type", ServerAuthTypes.LDAP.name)
                put("title", "MAGE")
                put("textColor", "#24de32")
                put("buttonColor", "#1E22E5")
            },
            onClick = {}
        )
    }
}

@Preview
@Composable
fun AuthButtonNoJsonPreview() {
    MaterialTheme {
        AuthStrategyButton(
            strategyJson = null,
            onClick = {}
        )
    }
}