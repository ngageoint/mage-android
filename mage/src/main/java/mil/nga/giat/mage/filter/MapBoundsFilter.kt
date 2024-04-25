package mil.nga.giat.mage.filter

//class MapBoundsFilter {
//    companion object {
//        fun filtersForBounds(
//            minLongitude: Double,
//            maxLongitude: Double,
//            minLatitude: Double,
//            maxLatitude: Double
//        ): List<Filter> {
//            return mutableListOf<Filter>().apply {
//                add(
//                    Filter(
//                        parameter = FilterParameter(
//                            type = FilterParameterType.DOUBLE,
//                            title = "Min Latitude",
//                            parameter = "latitude",
//                        ),
//                        comparator = ComparatorType.GREATER_THAN_OR_EQUAL,
//                        value = minLatitude
//                    )
//                )
//
//                add(
//                    Filter(
//                        parameter = FilterParameter(
//                            type = FilterParameterType.DOUBLE,
//                            title = "Min Longitude",
//                            parameter = "longitude",
//                        ),
//                        comparator = ComparatorType.GREATER_THAN_OR_EQUAL,
//                        value = minLongitude
//                    )
//                )
//
//                add(
//                    Filter(
//                        parameter = FilterParameter(
//                            type = FilterParameterType.DOUBLE,
//                            title = "Max Latitude",
//                            parameter = "latitude",
//                        ),
//                        comparator = ComparatorType.LESS_THAN_OR_EQUAL,
//                        value = maxLatitude
//                    )
//                )
//
//                add(
//                    Filter(
//                        parameter = FilterParameter(
//                            type = FilterParameterType.DOUBLE,
//                            title = "Max Longitude",
//                            parameter = "longitude",
//                        ),
//                        comparator = ComparatorType.LESS_THAN_OR_EQUAL,
//                        value = maxLongitude
//                    )
//                )
//            }
//        }
//    }
//}