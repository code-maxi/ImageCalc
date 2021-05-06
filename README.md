# ImageCalc

ImageCalc is a software written in JavaFX with which one can measure an image with paths. You can write a code pattern to convert the data into a text. You can also apply some filter.

![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc.png?raw=true)
<br>

# Get started
1. Download the JAR placed in `./jar`.

1. Run the JAR by double click or running the following command:
    
    `java -jar <path to JAR>/ImageCalc.jar <arguments>`
    
    You can specify multiple files to be opened as arguments. These can be folders as well as files.
<br>    

> **Note**
> Java 11 must be installed at least!

<br>

# GUI
## Opened Files
![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-files.png?raw=true)

At the right top there is a tree view of the opened files. To open one just click on the selected file. The list of paths is not deleted then. If you want to open a new file just click on open. If the open checkbox is selected you can open a folder.
<br>

## Path List
![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-paths.png?raw=true)

At the right bottom there is a list of the paths. There you can edit and hide them.

Control|Function
-|-
_visible checkbox_|It indicates whether the object is visible. If one is hidden it will be ignored from the whole software.
_first text field_|It specifies the name of the selected path.
_second text field_|It specifies the group of the selected paths.
_remove path_|The selected path is removed.
_remove point_|The point specified in the text field is removed.
<br>

## Editing Area
![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-canvas.png?raw=true)
On editing area you can generate the paths.

Action|Function
-|-
_left click_|add a point to the selected path
_mouse wheel click_|remove last point of the selected path
_right click_|create new path with one point

If the **snap** checkbox is selected you can snap to other points.
<br>

## Settings
![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-settings.png?raw=true)

### View Settings
Control|Function
-|-
_slider_|It indicates the size of the canvas.
_position checkbox_|Whether the position badge of a point is shown.
_length checkbox_|Whether the length badge between two points is shown.
_number checkbox_|Whether the number badge of a point is shown.
_line checkbox_|Whether the lines between two points is shown.
_points checkbox_|Whether the points are shown.
_only current_|Whether the badges are only shown on the selected path.
_snap_|Whether the mouse snaps to near points.


### Format Settings
||Function|Default Value|Note
-|-|:-:|-
_1st text field_|The calculated width.|**1.0**|If the value contains a dot it is multiplied with the width of the current image.
_2nd text field_|The calculated height.|**1.0**|If the value contains a dot it is multiplied with the height of the current image.
_3rd text field_|The X translation for the calculated coordinates.|**0**|If the value contains a dot it is multiplied with the width of the current image.
_4th text field_|The Y translation for the calculated coordinates.<br>|**0**|If the value contains a dot it is multiplied with the height of the current image.
_5th text field_|The number of the decimal places.|**0**

> **Note**
> If a value is invalid, the default value is used.
> The text fields only bear on the formated text.

> **Example**
> The values of the text fields are `1` `1` `-0.5` `-0.5` `3`.
> The image resolution is `10x10`.
> The point `P(2.5|7.5)` would be formated to `P(-0.250|0.250)`.

<br>

## Formatting the Data

![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-pattern.png?raw=true)

You can format the data to a text. When you write a code pattern in the textarea it is directly formated to a text shown below.

### Syntax
`< ... >` is a for-each loop for all paths. So everything between `<` and `>` is applied to all paths. You can use ``` ` ... ` ``` to create a for-each loop for the points on the path. You can also use filters in such a for-each loop that you specify after the first operator in square brackets. You can seperate them with `|`.

You can use the following variables and filters in the code pattern.

Variable|Meaning|Availability
-|-|-
`#{rw}` , `#{rh}`|width and height of the image resolution|_everywhere_
`#{path}`|path to the image|_everywhere_
`#{name}`|name of the image|_everywhere_
`#{extension}`|extension of the image|_everywhere_
`#{i}`|index of item|_in any for-each_
`#{i2}`|index + 1 of item|_in any for-each_
`#{c}`|color of path|_in path for-each_
`#{n}`|name of path|_in path for-each_
`#{g}`|group of path|_in path for-each_
`#{mx}` , `#{mx}`|coordinates of the center of an open path|_in path for-each_
`#{ml}`|maximum length from center of an open path to its points|_in path for-each_
`#{m2x}` , `#{m2x}`|coordinates of the center of a closed path|_in path for-each_
`#{m2l}`|maximum length from center of a closed path to its points|_in path for-each_
`#{x}` , `#{y}`|coordinates of a point|_in point for-each_
`#{dx}` , `#{dy}`|coordinates of the delta vector between a point and its next|_in point for-each_
`#{dl}` , `#{da}`|length and angle  (radian measure) of the delta vector between a point and its next|_in point for-each_

Filter|Meaning|Note
-|-|-
`nth: <n>`|Only selects the `n`-th item in the list (from the beginning).
`last-nth: <n>`|Only selects the `n`-th item in the list (from the end).
`range:<a>-<b>`|Only selects the items in the range of `a` (from the beginning) until `b` (from the end).
`name:<t>`|Only selects paths with the name `t`.|_only aviable for path for-each_
`group:<g>`|Only selects paths with the group `g`.|_only aviable for path for-each_

### Example
If you have the following data  and if you didn't change the format settings ...
```
image resolution: 10x10
paths:

* [b] path 1 (orange)
    - (0 | 0)
    - (1 | 5)
    
* [a] path 2  (blue)
    - (0 | 0)
    - (2 | 3)    
    
* [b] path 3 (red)
    - (7 | 8)
    - (5 | 4)
    - (7 | 4)

* [b] path 4 (green)
    - (1 | 1)
    - (3 | 4)
    
```
... the code pattern ...
```
resolution: #{rw}x#{rh}

<[range:0-1|group]#{n} (#{c}):
`    [range:0-1]delta vector between point #{i} and point #{i2}: (#{dx} | #{dy})
`>
```
... would be formatted to ...
```
resolution: 10x10

path 1 (orange):
    delta vector between point 0 and point 1: (1 | 5)
    
path 3 (red):
    delta vector between point 0 and point 1: (-2 | -4)
    delta vector between point 1 and point 2: (2 | 0)
```
<br>

## Save Data
![](https://github.com/code-maxi/ImageCalc/blob/master/screenshots/image-calc-coded-data.png?raw=true)

At the bottom left is a textarea that contains the whole data as a serialized text. If the data is changed, the text area is automatically updated. If you click on **update** the whole data is reloaded from the text. When you click on **save** the text in the text are is just saved to a specified file. By clicking **open** you can open such a file.
