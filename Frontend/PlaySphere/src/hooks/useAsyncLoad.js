import { useEffect, useState } from "react";

export default function useAsyncLoad(loader, deps = []) {
  const [state, setState] = useState({
    loading: true,
    data: null,
    error: null,
  });

  useEffect(() => {
    let active = true;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: null,
    }));

    loader()
      .then((data) => {
        if (active) {
          setState({
            loading: false,
            data,
            error: null,
          });
        }
      })

      .catch((error) => {
        if (active) {
          setState({
            loading: false,
            data: null,
            error: error.message,
          });
        }
      });

    return () => {
      active = false;
    };
  }, deps);

  return state;
}
