import React from 'react';
import VisibilityOffIcon from "@material-ui/icons/VisibilityOff"
import IconLabel from "../../IconLabel";

export default function EmptyDataView() {
  return <IconLabel icon={<VisibilityOffIcon/>} label="Content not rendered"/>
}
