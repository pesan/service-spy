import {Observable} from "rxjs"

export function fromEventSource(uri) {
  return Observable.create(observer => {
    const evtSource = new EventSource(uri)

    evtSource.onmessage = observer.next.bind(observer)
    evtSource.onerror = observer.error.bind(observer)

    return () => {
      evtSource.close()
    }
  })
}
