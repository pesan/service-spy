import React from "react";
import Preformatted from "./PreFormattedDataView"

const formatStackTraceItem = traceItem => {
  const lineNumber = traceItem.nativeMethod ? '<native>' : traceItem.lineNumber;
  return `  at ${traceItem.className}.${traceItem.methodName}(${traceItem.fileName}:${lineNumber})`
}

export default function ExceptionDataView({exception}) {
  return <Preformatted style={{color: 'red'}}>
    {exception.name}: {exception.message}{"\n"}
    {exception.stackTrace.map(formatStackTraceItem).join("\n")}
  </Preformatted>
}

