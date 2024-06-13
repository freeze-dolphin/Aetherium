package io.sn.aetherium.objects.serialiation

import com.tairitsu.compose.arcaea.Chart
import com.tairitsu.compose.arcaea.pos
import io.sn.aetherium.objects.ShardDigestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class UnionSerializerTest {

    @Test
    fun `test Union serialization`() {
        "timing" testType ShardDigestion.Union.Restriction.editorDelayTiming(1000L)
        "position" testType ShardDigestion.Union.Restriction.fromPosition(1.0 pos 2.5)
        "int" testType ShardDigestion.Union(233)
        "long" testType ShardDigestion.Union(2333L)
        "double" testType ShardDigestion.Union(233.333)
        "doubleArr" testType ShardDigestion.Union(arrayOf(1.0, 2.0, 3.5, 4.6))
        "stringArr" testType ShardDigestion.Union("Two three three")
        "globalOffsetPh" testType ShardDigestion.Union.Restriction.songGlobalOffsetPlaceholder
        "timingAterSixSec" testType ShardDigestion.Union.Restriction.editorDelayTiming(6000)

        /*
        timing - {"timingValue":-601000}
        position - {"positionValue":[1.0,2.5]}
        int - {"intValue":233}
        long - {"longValue":2333}
        double - {"doubleValue":233.333}
        doubleArr - {"doubleArrayValue":[1.0,2.0,3.5,4.6]}
        stringArr - {"stringValue":"Two three three"}
        chart - {"chartValue":{"mainTiming":{"notes":[{"type":"normal","time":1,"column":2},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":17140,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":true,"tapList":[17140,19045]},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]}],"scenecontrols":[{"time":19045,"type":"TRACK_HIDE","param1":null,"param2":null},{"time":40960,"type":"RED_LINE","param1":1.88,"param2":0}]},"subTiming":{"709fc929-6c99-4459-b809-848551c74699":{"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"hold","time":17140,"endTime":18807,"column":4},{"type":"arc","time":17140,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"si","endPosition":[1.0,1.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[17140,19045]},{"type":"arc","time":17140,"endTime":18569,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18569]},{"type":"arc","time":17140,"endTime":18093,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.25,0.25],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18093]},{"type":"arc","time":17140,"endTime":17616,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.0,0.5],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"hold","time":19045,"endTime":20712,"column":4},{"type":"arc","time":19045,"endTime":20712,"startPosition":[-0.25,1.0],"curveType":"b","endPosition":[1.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"arc","time":19045,"endTime":20712,"startPosition":[1.25,1.0],"curveType":"b","endPosition":[-0.5,0.0],"color":1,"hitSound":"none","isGuidingLine":false,"tapList":[]}]}}}}
         */
        "chart" testType ShardDigestion.Union(
            Chart.fromAff(
                """
                scenecontrol(19045,trackhide);
                (1,2);
                arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
                scenecontrol(40960,redline,1.88,0);
                arc(17140,19045,0.00,1.00,s,1.00,1.00,3,none,false)[arctap(17140),arctap(19045)];
                arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
                timinggroup(fadingholds_anglex3600){
                    timing(0,126.00,4.00);
                    hold(17140,18807,4);
                    arc(17140,19045,0.00,1.00,si,1.00,1.00,0,none,true)[arctap(17140),arctap(19045)];
                    arc(17140,18569,0.00,0.50,siso,1.00,0.00,0,none,true)[arctap(18569)];
                    arc(17140,18093,0.00,0.25,siso,1.00,0.25,0,none,true)[arctap(18093)];
                    arc(17140,17616,0.00,0.00,siso,1.00,0.50,0,none,true);
                    hold(19045,20712,4);
                    arc(19045,20712,-0.25,1.50,b,1.00,0.00,0,none,true);
                    arc(19045,20712,1.25,-0.50,b,1.00,0.00,1,none,false);
                };
                """.trimIndent()
            )
        )

    }

    private infix fun String.testType(union: ShardDigestion.Union) {
        val json = Json.encodeToString(union)
        println("$this - $json")
        val reunion = Json.decodeFromString<ShardDigestion.Union>(json)
        assertEquals(json, Json.encodeToString(reunion))
    }

}