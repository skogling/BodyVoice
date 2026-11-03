package com.liangji.bodymeasure.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Tune: ImageVector by lazy {
        icon("Tune") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 17f)
                lineTo(20f, 17f)
            }
            fillPath { circle(9f, 7f, 2.2f) }
            fillPath { circle(15f, 12f, 2.2f) }
            fillPath { circle(8.5f, 17f, 2.2f) }
        }
    }

    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack") {
            strokePath {
                moveTo(15f, 5f)
                lineTo(8f, 12f)
                lineTo(15f, 19f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") {
            strokePath {
                moveTo(12f, 3f)
                lineTo(19f, 6f)
                lineTo(19f, 11f)
                curveTo(19f, 16f, 16f, 19.5f, 12f, 21f)
                curveTo(8f, 19.5f, 5f, 16f, 5f, 11f)
                lineTo(5f, 6f)
                close()
            }
        }
    }

    val Database: ImageVector by lazy {
        icon("Database") {
            strokePath {
                moveTo(5f, 6f)
                curveTo(5f, 4.3f, 8.1f, 3f, 12f, 3f)
                curveTo(15.9f, 3f, 19f, 4.3f, 19f, 6f)
                curveTo(19f, 7.7f, 15.9f, 9f, 12f, 9f)
                curveTo(8.1f, 9f, 5f, 7.7f, 5f, 6f)
                close()
                moveTo(5f, 6f)
                lineTo(5f, 18f)
                curveTo(5f, 19.7f, 8.1f, 21f, 12f, 21f)
                curveTo(15.9f, 21f, 19f, 19.7f, 19f, 18f)
                lineTo(19f, 6f)
                moveTo(5f, 12f)
                curveTo(5f, 13.7f, 8.1f, 15f, 12f, 15f)
                curveTo(15.9f, 15f, 19f, 13.7f, 19f, 12f)
            }
        }
    }

    val Info: ImageVector by lazy {
        icon("Info") {
            strokePath {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 16f)
            }
            fillPath { circle(12f, 8f, 1f) }
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            strokePath {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            strokePath {
                moveTo(7f, 7f)
                lineTo(17f, 17f)
                moveTo(17f, 7f)
                lineTo(7f, 17f)
            }
        }
    }

    val Backspace: ImageVector by lazy {
        icon("Backspace") {
            strokePath {
                moveTo(8.5f, 6f)
                lineTo(19f, 6f)
                lineTo(22f, 12f)
                lineTo(19f, 18f)
                lineTo(8.5f, 18f)
                lineTo(3f, 12f)
                close()
                moveTo(12f, 9.5f)
                lineTo(16f, 14.5f)
                moveTo(16f, 9.5f)
                lineTo(12f, 14.5f)
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            strokePath {
                moveTo(5f, 12.5f)
                lineTo(10f, 17.5f)
                lineTo(19f, 7f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            strokePath {
                moveTo(8f, 4f)
                lineTo(8f, 7f)
                moveTo(16f, 4f)
                lineTo(16f, 7f)
                moveTo(5f, 9f)
                lineTo(19f, 9f)
                moveTo(5f, 10f)
                lineTo(5f, 18f)
                curveTo(5f, 19.7f, 6.3f, 21f, 8f, 21f)
                lineTo(16f, 21f)
                curveTo(17.7f, 21f, 19f, 19.7f, 19f, 18f)
                lineTo(19f, 10f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            strokePath {
                moveTo(9f, 5f)
                lineTo(16f, 12f)
                lineTo(9f, 19f)
            }
        }
    }

    val Edit: ImageVector by lazy {
        icon("Edit") {
            strokePath {
                moveTo(5f, 19f)
                lineTo(4f, 15f)
                lineTo(15f, 4f)
                curveTo(15.6f, 3.4f, 16.6f, 3.4f, 17.2f, 4f)
                lineTo(20f, 6.8f)
                curveTo(20.6f, 7.4f, 20.6f, 8.4f, 20f, 9f)
                lineTo(9f, 20f)
                close()
                moveTo(13.5f, 5.5f)
                lineTo(18.5f, 10.5f)
            }
        }
    }

    val Archive: ImageVector by lazy {
        icon("Archive") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(5f, 7f)
                lineTo(5f, 19f)
                curveTo(5f, 20.1f, 5.9f, 21f, 7f, 21f)
                lineTo(17f, 21f)
                curveTo(18.1f, 21f, 19f, 20.1f, 19f, 19f)
                lineTo(19f, 7f)
                moveTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(16f, 7f)
                lineTo(8f, 7f)
                close()
                moveTo(9f, 11f)
                lineTo(15f, 11f)
            }
        }
    }

    val Trash: ImageVector by lazy {
        icon("Trash") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(9f, 7f)
                lineTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 7f)
                moveTo(6f, 7f)
                lineTo(7f, 19f)
                curveTo(7.1f, 20.1f, 8f, 21f, 9.1f, 21f)
                lineTo(14.9f, 21f)
                curveTo(16f, 21f, 16.9f, 20.1f, 17f, 19f)
                lineTo(18f, 7f)
                moveTo(10f, 11f)
                lineTo(10f, 17f)
                moveTo(14f, 11f)
                lineTo(14f, 17f)
            }
        }
    }

    val ArrowUp: ImageVector by lazy {
        icon("ArrowUp") {
            strokePath {
                moveTo(12f, 19f)
                lineTo(12f, 5f)
                moveTo(6f, 11f)
                lineTo(12f, 5f)
                lineTo(18f, 11f)
            }
        }
    }

    val ArrowDown: ImageVector by lazy {
        icon("ArrowDown") {
            strokePath {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(6f, 13f)
                lineTo(12f, 19f)
                lineTo(18f, 13f)
            }
        }
    }

    val ArrowToTop: ImageVector by lazy {
        icon("ArrowToTop") {
            strokePath {
                moveTo(12f, 19f)
                lineTo(12f, 9f)
                moveTo(7f, 14f)
                lineTo(12f, 9f)
                lineTo(17f, 14f)
                moveTo(6f, 5f)
                lineTo(18f, 5f)
            }
        }
    }

    val More: ImageVector by lazy {
        icon("More") {
            fillPath { circle(12f, 5f, 1.6f) }
            fillPath { circle(12f, 12f, 1.6f) }
            fillPath { circle(12f, 19f, 1.6f) }
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            strokePath {
                moveTo(12f, 4f)
                lineTo(12f, 15f)
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                moveTo(5f, 20f)
                lineTo(19f, 20f)
            }
        }
    }

    val Upload: ImageVector by lazy {
        icon("Upload") {
            strokePath {
                moveTo(12f, 16f)
                lineTo(12f, 5f)
                moveTo(7f, 10f)
                lineTo(12f, 5f)
                lineTo(17f, 10f)
                moveTo(5f, 20f)
                lineTo(19f, 20f)
            }
        }
    }

    val Table: ImageVector by lazy {
        icon("Table") {
            strokePath {
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
                moveTo(5f, 10f)
                lineTo(19f, 10f)
                moveTo(12f, 10f)
                lineTo(12f, 19f)
            }
        }
    }

    val Heart: ImageVector by lazy {
        icon("Heart") {
            strokePath {
                moveTo(12f, 21.35f)
                lineTo(10.55f, 20.03f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.03f)
                lineTo(12f, 21.35f)
                close()
            }
        }
    }
}

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

private fun ImageVector.Builder.fillPath(block: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        pathBuilder = block,
    )
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(
        radius,
        radius,
        0f,
        isMoreThanHalf = true,
        isPositiveArc = true,
        x1 = centerX + radius,
        y1 = centerY,
    )
    arcTo(
        radius,
        radius,
        0f,
        isMoreThanHalf = true,
        isPositiveArc = true,
        x1 = centerX - radius,
        y1 = centerY,
    )
    close()
}
