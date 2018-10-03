import React from "react";

export default function PreFormattedDataView({children, style}) {
  return <pre style={{
    maxHeight: 320,
    border: '1px solid lightgrey',
    borderRadius: '6px',
    overflow: 'auto',
    margin: '.2em',
    padding: '.5em 0 .5em .5em',
    ...style,
  }}>{children}</pre>
}
