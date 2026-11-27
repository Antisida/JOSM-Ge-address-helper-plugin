package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy

import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult


interface ResultStrategy {

    fun createCommand(dataSet: DataSet, result: ParseResult): List<Command>
    fun executeAdditionally(result: ParseResult)

}