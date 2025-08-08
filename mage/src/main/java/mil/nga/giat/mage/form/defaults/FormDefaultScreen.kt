package mil.nga.giat.mage.form.defaults

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.textButtonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.edit.FieldEditContent
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.ui.theme.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun FormDefaultScreen(
  event: Event?,
  formStateLiveData: LiveData<FormState?>,
  onClose: (() -> Unit)? = null,
  onSave: (() -> Unit)? = null,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  val formState by formStateLiveData.observeAsState()
  val scaffoldState = rememberScaffoldState()

  MageTheme {
    Scaffold(
      scaffoldState = scaffoldState,
      topBar = {
        TopBar(
          formName = formState?.definition?.name,
          onClose = { onClose?.invoke() },
          onSave = {
            onSave?.invoke()
          }
        )
      },
      content = {
        Content(
          event = event,
          formState = formState,
          onReset = {
            onReset?.invoke()
          },
          onFieldClick = onFieldClick
        )
      }
    )
  }
}

@Composable
fun TopBar(
  formName: String?,
  onClose: () -> Unit,
  onSave: () -> Unit
) {
  TopAppBar(
    modifier = Modifier
      .background(color = MaterialTheme.colors.topAppBarBackground)
      .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    backgroundColor = MaterialTheme.colors.topAppBarBackground,
    contentColor = Color.White,
    elevation = 0.dp,
    title = {
      Column {
        Text(formName ?: "")
      }
    },
    navigationIcon = {
      IconButton(onClick = { onClose.invoke() }) {
        Icon(Icons.Default.Close, stringResource(R.string.cancel_default))
      }
    },
    actions = {
      TextButton(
        colors = textButtonColors(contentColor = Color.White),
        onClick = { onSave() }
      ) {
        Text("SAVE")
      }
    }
  )
}


@Composable
fun Content(
  event: Event?,
  formState: FormState?,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  Surface {
    Column(
      Modifier
        .fillMaxHeight()
        .background(Color(0x19000000))
        .verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
        .padding(horizontal = 8.dp)
    ) {
      if (formState?.definition != null) {
        DefaultHeader(
          name = formState.definition.name,
          description = formState.definition.description,
          color = formState.definition.hexColor
        )

        DefaultContent(
          event = event,
          formState = formState,
          onReset = onReset,
          onFieldClick = onFieldClick
        )
      }
    }
  }
}

@Composable
fun DefaultHeader(
  name: String,
  description: String?,
  color: String
) {
  Row(
    modifier = Modifier.padding(vertical = 16.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Description,
      contentDescription = "Form",
      tint = Color(android.graphics.Color.parseColor(color)),
      modifier = Modifier
        .width(40.dp)
        .height(40.dp)
    )

    Column(
      Modifier
        .weight(1f)
        .padding(start = 16.dp)
        .fillMaxWidth()
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.h6
      )

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        if (description?.isNotEmpty() == true) {
          Text(
            text = description,
            style = MaterialTheme.typography.body2
          )
        }
      }
    }
  }
}

@Composable
fun DefaultContent(
  event: Event?,
  formState: FormState,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  Card(Modifier.padding(bottom = 16.dp)) {
    Column {
      Text(
        text = stringResource(R.string.custom_form_defaults),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(16.dp)
      )

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          text = stringResource(R.string.form_defaults_msg),
          style = MaterialTheme.typography.body2,
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
      }

      DefaultFormContent(
        event = event,
        formState = formState,
        onFieldClick = onFieldClick
      )

      Divider()

      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        TextButton(
          onClick = { onReset?.invoke() },
          colors = textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(text = stringResource(R.string.reset_defaults_msg))
        }
      }
    }
  }
}

@Composable
fun DefaultFormContent(
  event: Event?,
  formState: FormState,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  val fields = formState.fields
    .filter { it.definition.type != FieldType.ATTACHMENT }
    .sortedBy { it.definition.id }

  for (fieldState in fields) {
    FieldEditContent(
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
      event = event,
      fieldState = fieldState,
      onClick = { onFieldClick?.invoke(fieldState) }
    )
  }
}
