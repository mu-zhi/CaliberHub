import { Component, ReactNode } from 'react'

interface ErrorBoundaryProps {
    fallback?: ReactNode
    children: ReactNode
}

interface ErrorBoundaryState {
    hasError: boolean
    error?: Error
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props)
        this.state = { hasError: false }
    }

    static getDerivedStateFromError(error: Error) {
        return { hasError: true, error }
    }

    componentDidCatch(error: Error, info: any) {
        // eslint-disable-next-line no-console
        console.error('ErrorBoundary caught an error', error, info)
    }

    render() {
        if (this.state.hasError) {
            return this.props.fallback ?? (
                <div style={{ padding: 24 }}>
                    <h3>出现错误</h3>
                    <p>{this.state.error?.message}</p>
                </div>
            )
        }
        return this.props.children
    }
}

export default ErrorBoundary
